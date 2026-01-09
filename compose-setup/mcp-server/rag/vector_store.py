import chromadb
import os
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

def get_collection():
    logger.info("Connecting to ChromaDB vector store")

    chroma_host = os.getenv("CHROMA_HOST", "chromadb_rag")
    chroma_port = int(os.getenv("CHROMA_PORT", "8000"))

    client = chromadb.HttpClient(
        host=chroma_host,
        port=chroma_port
    )

    return client.get_or_create_collection(
        name="recipes"
    )
