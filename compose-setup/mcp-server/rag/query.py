from .vector_store import get_collection
from .embeddings import embed
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

def search_recipes(query: str, k: int = 5):
    """
    Perform a semantic search in ChromaDB for recipes matching the query.

    Args:
        query (str): The search query text.
        k (int): Number of top results to return.

    Returns:
        list of dict: Each dict contains recipe metadata and the text.
    """
    logger.info(f"Searching for recipes matching query: {query}")
    # Generate embedding for the query
    query_embedding = embed(query)

    # Get the Chroma collection
    collection = get_collection()

    # Query the collection
    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=k
    )

    # Format the results
    formatted_results = []
    for i in range(len(results['ids'][0])):
        formatted_results.append({
            'id': results['ids'][0][i],
            'text': results['documents'][0][i],
            'metadata': results['metadatas'][0][i]
        })

    return formatted_results
