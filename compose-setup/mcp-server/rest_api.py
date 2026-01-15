from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from langchain.agents import create_agent
from langchain_mcp_adapters.client import MultiServerMCPClient
from pydantic import BaseModel
import os
import httpx
import json
from langchain_openai import ChatOpenAI
import logging
import sys
from contextlib import asynccontextmanager
from rag.query import search_recipes
from fastapi.middleware.cors import CORSMiddleware

# from flask import Flask, jsonify, request

# The following code ingests the data to the vector database, run it once to populate the DB
# comment it out after the first run, otherwise it will dublicate the data every time the server starts
# from rag.ingest import ingest
# ingest('data/exampledata.json')

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)

logger = logging.getLogger(__name__)

llm_api_url = os.getenv('LLM_API_URL', 'http://ollama:11434/v1')
llm_model = os.getenv('LLM_MODEL', 'llama3.2:3b')
openai_api_key = os.getenv('OPENAI_API_KEY', 'ollama')

mcp_client = None
tools = None
agent = None

@asynccontextmanager
async def lifespan(restapi: FastAPI):
    global mcp_client, tools, agent
    mcp_client = MultiServerMCPClient(
        {
            "my_server": {
                "transport": "http",
                "url": f"http://localhost:{os.getenv('MCP_PORT','6006')}/mcp",
            }
        }
    )
    tools = await mcp_client.get_tools()
    logger.info(f"Available tools: {[t.name for t in tools]}")
    llm = ChatOpenAI(
        model=llm_model, 
        base_url=llm_api_url,
        api_key=openai_api_key
    )
    agent = create_agent(llm, tools)
    yield

restapi = FastAPI(title="Rest Api", version="1.0.0", lifespan=lifespan)
restapi.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],           # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],           # Allows all methods (GET, POST, etc.)
    allow_headers=["*"],           # Allows all headers
)

class PromptRequest(BaseModel):
    prompt: str

class CocktailRecommendRequest(BaseModel):
    tags: list[str]
    query: str

class ChromaSearchRequest(BaseModel):
    query: str
    count: int

@restapi.get('/hello')
async def hello_world():
    return {
        'message': 'Hello, World!',
        'status': 'success'
    }

@restapi.post('/generate')
async def generate_stream(request: PromptRequest):
    """
    Streams raw LLM response without MCP tools.
    Example: {"prompt": "Why is the sky blue?"}
    """
    if not request.prompt:
        raise HTTPException(status_code=400, detail='Missing or empty "prompt" field')

    openai_url = f"{llm_api_url}/chat/completions"
    openai_payload = {
        'model': llm_model,
        'messages': [{'role': 'user', 'content': request.prompt}],
        'stream': True
    }

    async def generate():
        try:
            async with httpx.AsyncClient(timeout=300.0) as client:
                headers = {"Authorization": f"Bearer {openai_api_key}"}
                async with client.stream('POST', openai_url, json=openai_payload, headers=headers) as response:
                    response.raise_for_status()

                    async for line in response.aiter_lines():
                        if line.startswith("data: "):
                            data_str = line[6:]
                            if data_str == "[DONE]":
                                break
                            try:
                                chunk = json.loads(data_str)
                                if 'choices' in chunk and len(chunk['choices']) > 0:
                                    delta = chunk['choices'][0].get('delta', {})
                                    if 'content' in delta:
                                        yield delta['content']
                            except json.JSONDecodeError:
                                continue

        except httpx.HTTPError as e:
            yield f"\n\nError: Failed to connect to LLM service: {str(e)}"
        except Exception as e:
            yield f"\n\nError: {str(e)}"

    return StreamingResponse(
        generate(),
        media_type='text/plain',
        headers={
            'Cache-Control': 'no-cache',
            'X-Accel-Buffering': 'no'
        }
    )

@restapi.post('/chat')
async def chat(request: PromptRequest):
    """
    Uses LangGraph agent with MCP tools to answer questions.
    Example: {"prompt": "Give me a cocktail recipe"}
    """
    if not request.prompt:
        raise HTTPException(status_code=400, detail='Missing or empty "prompt" field')

    if agent is None:
        raise HTTPException(status_code=503, detail="Agent not initialized")

    try:
        # Run agent
        result = await agent.ainvoke(
            {"messages": [
                {"role": "system", "content": "You are a helpful assistant. When asked about cocktails or recipes, use the search_for_cocktail_recipes_in_db tool to get the recipe."},
                {"role": "user", "content": request.prompt}
            ]}
        )

        return {
            "response": result["messages"][-1].content,
            "status": "success"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")

@restapi.post('/recommend_cocktail')
async def recommend_cocktail(request: CocktailRecommendRequest):
    """
    Uses the LangChain agent + MCP tools to recommend a cocktail.

    Input example:
      {
        "tags": ["sweet", "refreshing", "ananas"],
        "query": "I'd like recommendation for a simple summer cocktail based on rum"
      }

    Requirements:
    - If not cocktail-related: respond that this service only offers cocktail recipes.
    - Always respond with a full cocktail recipe.
    - Never ask follow-up questions.
    """
    if agent is None:
        raise HTTPException(status_code=503, detail="Agent not initialized")

    tags = request.tags or []
    query = (request.query or "").strip()

    if not query:
        raise HTTPException(status_code=400, detail='Missing or empty "query" field')

    tags_text = ", ".join([t.strip() for t in tags if t and t.strip()])

    system_prompt = (
        "You are a cocktail recipe assistant."
        "\n\nRules (must follow):"
        "\n1) When selecting or generating a recipe, you should use available tools."
        "\n2) Always reply with ONE cocktail recipe."
        "\n3) Never ask a follow-up question. Make reasonable assumptions instead."
        "\n4) Give every cocktail a name and wrap this name in h2-tag for valid html code."
        "\n5) Your response should immediately start with the h2 tag and the title of the cocktail."
        "\n6) For each cocktail, create a title, a bullet point list of ingredients and a bullet point list on the method."
        "\n7) Explicitly name both bullet point lists by adding a title above each bullet point list."
        "\n8) Wrap the bullet points into valid ul or li tags and produce valid HTML markup"
    )

    # Construct a single user message that includes both tags and query.
    user_prompt = (
        "Return the best matching cocktail for the following request:\n"
        f"- Desired features: {tags_text if tags_text else 'none'}\n"
        f"- Description: {query}\n\n"
    )

    logger.info(f"User prompt: {user_prompt}")

    try:
        result = await agent.ainvoke(
            {"messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ]}
        )

        return {
            "response": result["messages"][-1].content,
            "status": "success"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")
    
    
# Added for RAG semantic search
@restapi.post("/rag/search")
def rag_search(request: ChromaSearchRequest):
    query = request.query
    n = request.count

    results = search_recipes(query, n)
    return results

if __name__ == '__main__':
    import uvicorn
    host = os.getenv("HOST", "0.0.0.0")
    rest_api_port = int(os.getenv("REST_API_PORT", 5005))
    uvicorn.run(restapi, host=host, port=rest_api_port)
