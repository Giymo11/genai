# MCP Server

A simple Python REST API server with a hello world endpoint.

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

## Running the Server

```bash
python server.py
```

The server will start on `http://localhost:5005`

## Testing the Endpoint

### Using curl

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

### Using a browser

Open your browser and navigate to:
```
http://localhost:5005/hello
```

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
docker run -p 5005:5005 genai-mcp-server:latest
```

The server will be accessible at `http://localhost:5005/hello`.

### Stop the Container

Find the container ID:
```bash
docker ps
```

Stop the container:
```bash
docker stop <container_id>
```

