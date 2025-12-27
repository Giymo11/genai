import chromadb
import os

client = chromadb.HttpClient(
    host=os.getenv("CHROMA_HOST", "chromadb_rag"),
    port=int(os.getenv("CHROMA_PORT", 8000))
)

collection = client.get_or_create_collection("recipes")

def add_recipe(recipe_id, text, embedding, metadata):
    collection.add(
        ids=[recipe_id],
        documents=[text],
        embeddings=[embedding],
        metadatas=[metadata]
    )

def search(query_embedding, k=5):
    return collection.query(
        query_embeddings=[query_embedding],
        n_results=k
    )
