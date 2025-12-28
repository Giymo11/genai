# Compose Setup

This directory contains a Docker Compose setup that orchestrates multiple services:
- **MCP Server**: A Python REST API server running on port 5005
- **React App**: A frontend application running on port 5050
- **ChromaDB (Vector DB for RAG)**: Running on port 8001

## Prerequisites

You need to have Docker and Docker Compose installed on your system.

### Installing Docker and Docker Compose

Install Docker Desktop from the official website (this installs both Docker and Docker Compose):
https://docs.docker.com/get-started/introduction/get-docker-desktop/

## Running the Setup

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
1. Build the Docker images for both services (if not already built)
2. Start the MCP server on `http://localhost:5005`
3. Start the React app on `http://localhost:5050`

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
docker compose logs frontend
```

### Rebuild the Services

If you make changes to the code, rebuild the images:

```bash
docker compose build
```

Then restart the services:

```bash
docker compose up
```

Or do both in one command:

```bash
docker compose up --build
```

## Accessing the Services

- **MCP Server API**: http://localhost:5005
  - Test endpoint: http://localhost:5005/hello
- **React Frontend**: http://localhost:5050

## Service Details

### MCP Server
- **Container Name**: genai-mcp-server
- **Port**: 5005
- **Source**: `./mcp-server`

### React App
- **Container Name**: genai-react-app
- **Port**: 5050
- **Source**: `./react-app`
- **API Connection**: Communicates with MCP server via internal Docker network

## Troubleshooting

### Port Already in Use

If you get an error about ports already being in use, you can either:

1. Stop the process using that port
2. Change the port mapping in `docker-compose.yml`:
   ```yaml
   ports:
     - "NEW_PORT:5005"  # For MCP server
     - "NEW_PORT:5050"  # For React app
   ```

### Permission Denied (Linux)

If you get permission errors on Linux, make sure your user is in the docker group:

```bash
sudo usermod -aG docker $USER
```

Then log out and back in.

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

