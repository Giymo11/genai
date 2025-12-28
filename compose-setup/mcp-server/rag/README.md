# RAG (Retrieval-Augmented Generation)

This folder contains all the code for **RAG functionality** in the MCP server: ingesting cocktail recipe data into a vector database (ChromaDB), generating embeddings, and enabling semantic search.

---

## File Overview

### 1. `ingest.py`
- **Purpose**: Reads JSON recipe data, converts it to text, generates embeddings, and stores them in ChromaDB.
- **How it works**:
  - Loads JSON files from the `data/` folder.
  - Converts each recipe to a text string (`recipe_to_text()`)
  - Generates embeddings using `embeddings.py`
  - Stores documents and metadata in ChromaDB using `vector_store.py`
- **Usage example**:

```bash
docker compose exec mcp python -c "from rag.ingest import ingest; ingest('data/exampledata.json')"
```

### 2. `vector_store.py`
- **Purpose**: Provides an interface to connect to ChromaDB and manage collections.
- **Key function: get_collection()**:
  - Uses environment variables for host/port:
    - CHROMA_HOST (default: chromadb_rag)
    - CHROMA_PORT (default: 8000)
  - Returns a collection object for ingestion and querying.
  Note: No global state at import time


### 3. `embeddings.py`
- **Purpose**: Generates vector embeddings from text for storage in ChromaDB.
- **Key points**:
  - Uses the sentence-transformers model "all-MiniLM-L6-v2"
  - Converts embeddings to Python lists for ChromaDB


### 4. `query.py`
- **Purpose**: Performs semantic search over the recipes stored in ChromaDB. Lets the MCP server or LLM query the vector database.
- **Functionality**:
  - search_recipes(query: str, k: int = 5) → returns top-k matching recipes
  - Will be called from MCP endpoints for LLM retrieval
- **Usage**:
```bash
docker compose exec mcp python -c "from rag.query import search_recipes; results = search_recipes('whiskey'); print(results)"
```
It prints recipes received by the query search.


### 5. `server.py`
- **Purpose**: Perform a semantic search on ingested recipes using ChromaDB embeddings.
- **Method**: GET
- **Query Parameters**:
  - q (string, required): The search query text (e.g., "whiskey cocktails").
  - k (integer, optional, default=5): Number of top results to return.
- **Usage**:
```bash
curl "http://localhost:5005/rag/search?q=whiskey&k=3" -UseBasicParsing
```
  

## Data Requirements

- All json files should be placed in the data/ folder at the root for the project (genai)
- Example supported json format:
```bash
{
  "cocktails": [
    { "name": "Whiskey Highball", "method": "stir", "serve": "rocks", "ingredients": [...] },
    { "name": "Gin and Tonic", "method": "stir", "serve": "rocks", "ingredients": [...] }
  ]
}
```

## Running RAG ingestion

this process can also be found in the README file for compose setup since it will be run there.

1. Start Docker containers including RAG setup
```bash
docker compose -f docker-compose.yml -f docker-compose.rag.yml up --build -d
```

2. Ingest JSON data
```bash
docker compose exec mcp python -c "from rag.ingest import ingest; ingest('data/exampledata.json')"
```
- Output indicates how many recipes were ingested (exist in json file)
- ChromaDB is now ready for semantic search

## Notes

- Any modifications in python files in rag folder require a Docker rebuild.
- The MCP server npw supports RAG generation using ChromaDB.