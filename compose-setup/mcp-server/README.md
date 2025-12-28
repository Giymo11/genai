# MCP Server 

This component contains two Python servers:
- **REST API** (`rest_api.py`): FastAPI server that handles user requests and orchestrates LLM + MCP tools
- **MCP Server** (`mcp_server.py`): FastMCP server that provides tools (e.g., cocktail recipes)

## Ports

| Port | Server | Description |
|------|--------|-------------|
| 5005 | REST API | User-facing API endpoints |
| 6006 | MCP Server | Internal MCP tools endpoint |

## Setup

You can set up the environment using either **venv** (standard Python) or **conda**.

### Option 1: Using venv (Standard Python)

#### 1. Create a Python Virtual Environment

```bash
python3 -m venv venv
```

#### 2. Activate the Virtual Environment

```bash
source venv/bin/activate
```

#### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

### Option 2: Using Conda

#### 1. Create a Conda Environment

```bash
conda create -n mcp-server python=3.11
```

#### 2. Activate the Conda Environment

```bash
conda activate mcp-server
```

#### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

## Running the Servers

### Run Both Servers (Recommended)

Use the entrypoint script to run both servers together:

```bash
./entrypoint.sh
```

### Run Servers Individually

**REST API:**
```bash
python rest_api.py
```
Starts on `http://localhost:5005`

**MCP Server:**
```bash
python mcp_server.py
```
Starts on `http://localhost:6006`

## Testing the Endpoints

### REST API (port 5005)

**Hello endpoint:**
```bash
curl http://localhost:5005/hello
```

Expected response:
```json
{
  "message": "Hello, World!",
  "status": "success"
}
```

**Chat with MCP tools:**
```bash
curl -X POST http://localhost:5005/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Give me a cocktail recipe"}'
```

### Using a browser

Open your browser and navigate to:
- REST API docs: `http://localhost:5005/docs`
- Hello endpoint: `http://localhost:5005/hello`

## Deactivating the Environment

When you're done, you can deactivate the environment:

**For venv:**
```bash
deactivate
```

**For conda:**
```bash
conda deactivate
```

## Running with Docker

### Build the Docker Image

```bash
./build.sh
```

This will create a Docker image named `genai-mcp-server:latest`.

### Run the Container

```bash
docker run -p 5005:5005 -p 6006:6006 genai-mcp-server:latest
```

The servers will be accessible at:
- REST API: `http://localhost:5005`
- MCP Server: `http://localhost:6006/mcp`

### Stop the Container

Find the container ID:
```bash
docker ps
```

Stop the container:
```bash
docker stop <container_id>
```
