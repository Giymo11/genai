# Compose Setup

This directory contains a Docker Compose setup that orchestrates three services:
- **MCP Server**: Runs a REST API at port 5005 and an MCP Server at (port 6006)
- **Ollama**: Local LLM service running Llama 3.2 (port 11434)
- **React App**: A frontend application (port 5050)

## Architecture

```
┌─────────────────┐     ┌─────────────────────────────────────┐     ┌─────────────────┐
│                 │     │         MCP Component               │     │                 │
│   React App     │────▶│  ┌───────────┐  ┌───────────────┐   │     │     Ollama      │
│   (port 5050)   │     │  │ REST API  │  │  MCP Server   │   │────▶│   (port 11434)  │
│                 │     │  │ (5005)    │──│  (6006)       │   │     │   Llama 3.2     │
└─────────────────┘     │  └───────────┘  └───────────────┘   │     └─────────────────┘
                        └─────────────────────────────────────┘
```

## Services

### MCP server (genai-mcp-server)
Contains two servers running in the same container:

| Server | Port | Description |
|--------|------|-------------|
| REST API | 5005 | FastAPI server that handles user requests and orchestrates LLM + tools |
| MCP Server | 6006 | FastMCP server that provides tools (e.g., cocktail recipes) |

**Environment Variables:**
- `REST_API_PORT=5005` - Port for the REST API
- `MCP_PORT=6006` - Port for the MCP tools server
- `LLM_API_URL=http://ollama:11434` - URL to reach Ollama
- `LLM_MODEL=llama3.2:3b` - The LLM model to use

### Ollama (genai-ollama)
Local LLM service that runs the Llama 3.2 model.

| Port | Description |
|------|-------------|
| 11434 | Ollama API endpoint |

**Features:**
- Automatically pulls the configured model on startup
- Persists model data in a Docker volume (`ollama-data`)

### React App (genai-react-app)
Frontend application that communicates with the REST API.

| Port | Description |
|------|-------------|
| 5050 | React development server |

## Prerequisites

You need to have Docker and Docker Compose installed on your system.

### Installing Docker and Docker Compose

Install Docker Desktop from the official website (this installs both Docker and Docker Compose):
https://docs.docker.com/get-started/introduction/get-docker-desktop/

## Running the Setup

### Start the Services

Navigate to this directory and run:

```bash
docker compose up
```

To run in detached mode (in the background):

```bash
docker compose up -d
```

To re-build the images before starting:

```bash
docker compose up --build
```

This will:
1. Build and start the Ollama service (pulls the LLM model if not present)
2. Build and start the MCP component (REST API + MCP Server)
3. Build and start the React app

### Stop the Services

If running in the foreground, press `Ctrl+C`.

If running in detached mode:

```bash
docker compose down
```

### View Logs

To view logs from all services:

```bash
docker compose logs
```

To follow logs in real-time:

```bash
docker compose logs -f
```

To view logs for a specific service:

```bash
docker compose logs mcp
docker compose logs ollama
docker compose logs frontend
```

### Rebuild the Services

If you make changes to the code, rebuild the images:

```bash
docker compose build
```

Or rebuild and start in one command:

```bash
docker compose up --build
```

## Accessing the Services

### REST API
- **Base URL**: http://localhost:5005
- **Hello endpoint**: http://localhost:5005/hello
- **Generate (streaming)**: POST http://localhost:5005/generate
- **Chat (with MCP tools)**: POST http://localhost:5005/chat
- **Swagger UI**: http://localhost:5005/docs
- **ReDoc**: http://localhost:5005/redoc

### MCP Server
- **Base URL**: http://localhost:6006/mcp
- Provides tools that the LLM can use (e.g., `provide_cocktail_recipe`)

### Ollama
- **API URL**: http://localhost:11434
- **List models**: http://localhost:11434/api/tags

### React Frontend
- **URL**: http://localhost:5050

## Testing the Setup

### 1. Check if services are running

```bash
# Test REST API
curl http://localhost:5005/hello

# Test Ollama
curl http://localhost:11434/api/tags
```

### 2. Test direct LLM generation (no tools)

```bash
curl -X POST http://localhost:5005/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is 2+2?"}'
```

### 3. Test chat with MCP tools

```bash
curl -X POST http://localhost:5005/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Give me a cocktail recipe"}'
```

This should trigger the `provide_cocktail_recipe` tool and return a recipe.

## Troubleshooting

### Port Already in Use

If you get an error about ports already being in use, you can either:

1. Stop the process using that port
2. Change the port mapping in `docker-compose.yml`

### Ollama Model Not Loading

Check if the model is being pulled:

```bash
docker compose logs ollama
```

You can also manually pull a model:

```bash
docker exec -it genai-ollama ollama pull llama3.2:3b
```

### MCP Tools Not Being Called

1. Check the logs for tool discovery:
   ```bash
   docker compose logs mcp | grep -i "tools"
   ```

2. Ensure both servers started successfully:
   ```bash
   docker compose logs mcp | grep -E "\[REST API\]|\[MCP\]"
   ```

### Services Not Starting

Check the logs for specific errors:

```bash
docker compose logs
```

### Reset Everything

To completely remove all containers, networks, and volumes:

```bash
docker compose down -v
```

Then rebuild and start:

```bash
docker compose up --build
```

**Note:** Using `-v` will also remove the Ollama model data, requiring it to be re-downloaded.









### Build and run using RAG override

```bash
docker compose -f docker-compose.yml -f docker-compose.rag.yml up --build -d
```

Confirm it's running:

```bash
docker compose ps
```
or
```bash
docker logs chromadb_rag
```

Check that the data is mounted:

```bash
docker compose exec mcp ls /app/data
```

__Ingesting Data into ChromaDB (RAG):__

Once the containers are running, we ingest the JSON dataset (exampledata.json in this case) by running:
```bash
docker compose exec mcp python -c "from rag.ingest import ingest; ingest('data/exampledata.json')"
```
This reads the json data file, generates embeddings and stores then in ChromaDB (vector database).
The output will run the MCP server and show how many recipes were ingested.

