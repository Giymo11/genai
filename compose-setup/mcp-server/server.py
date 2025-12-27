from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import os
import httpx
import json

app = FastAPI(title="MCP Server", version="1.0.0")


class PromptRequest(BaseModel):
    prompt: str


@app.get('/hello')
async def hello_world():
    return {
        'message': 'Hello, World!',
        'status': 'success'
    }


@app.post('/generate')
async def generate(request: PromptRequest):
    """
    Accepts a JSON object with a 'prompt' field and streams the LLM response.
    Example: {"prompt": "Why is the sky blue?"}
    """
    if not request.prompt:
        raise HTTPException(status_code=400, detail='Missing or empty "prompt" field')

    # Get LLM configuration from environment variables
    llm_api_url = os.getenv('LLM_API_URL', 'http://ollama:11434')
    llm_model = os.getenv('LLM_MODEL', 'llama3.2:3b')

    # Prepare the request to Ollama API
    ollama_url = f"{llm_api_url}/api/generate"
    ollama_payload = {
        'model': llm_model,
        'prompt': request.prompt,
        'stream': True
    }

    async def generate_stream():
        """Generator function that streams only the response text from Ollama"""
        try:
            # Make streaming request to Ollama using httpx
            async with httpx.AsyncClient(timeout=300.0) as client:
                async with client.stream('POST', ollama_url, json=ollama_payload) as response:
                    response.raise_for_status()

                    # Process each line from the stream
                    async for line in response.aiter_lines():
                        if line:
                            try:
                                # Parse the JSON response
                                chunk = json.loads(line)

                                # Extract only the 'response' field and stream it
                                if 'response' in chunk:
                                    yield chunk['response']

                                # Check if generation is complete
                                if chunk.get('done', False):
                                    break
                            except json.JSONDecodeError:
                                continue

        except httpx.HTTPError as e:
            yield f"\n\nError: Failed to connect to LLM service: {str(e)}"
        except Exception as e:
            yield f"\n\nError: {str(e)}"

    # Return streaming response with text/plain content type
    return StreamingResponse(
        generate_stream(),
        media_type='text/plain',
        headers={
            'Cache-Control': 'no-cache',
            'X-Accel-Buffering': 'no'
        }
    )


if __name__ == '__main__':
    import uvicorn
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 5005))
    uvicorn.run(app, host=host, port=port)
