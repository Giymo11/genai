from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from langchain.agents import create_agent
from langchain_mcp_adapters.client import MultiServerMCPClient
from pydantic import BaseModel
import os
import httpx
import json
from langchain_ollama import ChatOllama
import logging
import sys
from contextlib import asynccontextmanager

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)

logger = logging.getLogger(__name__)

llm_api_url = os.getenv('LLM_API_URL', 'http://ollama:11434')
llm_model = os.getenv('LLM_MODEL', 'llama3.2:3b')

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
    llm = ChatOllama(model=llm_model, base_url=llm_api_url)
    agent = create_agent(llm, tools)
    yield

restapi = FastAPI(title="Rest Api", version="1.0.0", lifespan=lifespan)

class PromptRequest(BaseModel):
    prompt: str

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

    ollama_url = f"{llm_api_url}/api/generate"
    ollama_payload = {
        'model': llm_model,
        'prompt': request.prompt,
        'stream': True
    }

    async def generate():
        try:
            async with httpx.AsyncClient(timeout=300.0) as client:
                async with client.stream('POST', ollama_url, json=ollama_payload) as response:
                    response.raise_for_status()

                    async for line in response.aiter_lines():
                        if line:
                            try:
                                chunk = json.loads(line)
                                if 'response' in chunk:
                                    yield chunk['response']
                                if chunk.get('done', False):
                                    break
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

    try:
        # Run agent
        result = await agent.ainvoke(
            {"messages": [
                {"role": "system", "content": "You are a helpful assistant. When asked about cocktails or recipes, use the provide_cocktail_recipe tool to get the recipe."},
                {"role": "user", "content": request.prompt}
            ]}
        )

        return {
            "response": result["messages"][-1].content,
            "status": "success"
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")

if __name__ == '__main__':
    import uvicorn
    host = os.getenv("HOST", "0.0.0.0")
    rest_api_port = int(os.getenv("REST_API_PORT", 5005))
    uvicorn.run(restapi, host=host, port=rest_api_port)
