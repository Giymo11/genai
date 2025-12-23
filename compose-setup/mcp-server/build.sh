#!/bin/bash

IMAGE_NAME="genai-mcp-server"
IMAGE_TAG="latest"

echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}..."

if docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .; then
    echo "Docker image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo "To run the container:"
    echo "  docker run -p 5005:5005 ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo "To access the server:"
    echo "  curl http://localhost:5005/hello"
else
    echo "Docker build failed."
    exit 1
fi

