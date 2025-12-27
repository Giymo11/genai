FROM python:3.11-slim

WORKDIR /app

# Copy requirements first for better layer caching
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt
# Added for RAG/embeddings
RUN pip install --no-cache-dir fastapi uvicorn chromadb sentence-transformers

# Copy the rest of the application
COPY . .

# Expose MCP server port
EXPOSE 5005

# Run the server
CMD ["python", "server.py"]


