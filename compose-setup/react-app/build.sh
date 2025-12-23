#!/bin/bash

IMAGE_NAME="genai-react-app"
IMAGE_TAG="latest"

echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}..."

if docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .; then
    echo "Docker image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo "To run the container:"
    echo "  docker run -p 5050:5050 ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo "To access the app:"
    echo "  Open http://localhost:5050 in your browser"
else
    echo "Docker build failed."
    exit 1
fi

