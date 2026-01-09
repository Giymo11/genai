#!/bin/bash

# Exit on error
set -xe

# Ensure Python output is unbuffered
export PYTHONUNBUFFERED=1

# Get configuration from environment variables
HOST="${HOST:-0.0.0.0}"
REST_API_PORT="${REST_API_PORT:-5005}"
MCP_PORT="${MCP_PORT:-6006}"

echo "Starting servers..."
echo "REST API will run on ${HOST}:${REST_API_PORT}"
echo "MCP Server will run on ${HOST}:${MCP_PORT}"

# Start MCP Server in background with prefix (using unbuffered python)
python -u mcp_server.py 2>&1 | sed -u 's/^/[MCP] /' &
MCP_PID=$!

# Wait for MCP server to fully initialize before starting REST API
echo "Waiting for MCP server to initialize..."
sleep 5

# Start REST API in background with prefix (using unbuffered python)
python -u rest_api.py 2>&1 | sed -u 's/^/[REST API] /' &
REST_PID=$!

# Function to handle shutdown
shutdown() {
    echo "Shutting down servers..."
    kill $REST_PID $MCP_PID 2>/dev/null
    wait $REST_PID $MCP_PID 2>/dev/null
    echo "Servers stopped"
    exit 0
}

# Trap SIGTERM and SIGINT
trap shutdown SIGTERM SIGINT

# Wait for both processes
wait $REST_PID $MCP_PID
