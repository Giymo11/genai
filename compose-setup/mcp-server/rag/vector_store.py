import chromadb
import os


def get_collection():
    chroma_host = os.getenv("CHROMA_HOST", "chromadb_rag")
    chroma_port = int(os.getenv("CHROMA_PORT", "8000"))

    client = chromadb.HttpClient(
        host=chroma_host,
        port=chroma_port
    )

    return client.get_or_create_collection(
        name="recipes"
    )
