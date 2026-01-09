import json
from .embeddings import embed
from .vector_store import get_collection
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

def recipe_to_text(r: dict) -> str:
    logger.info("Converting recipe to text format")
    ingredients_text = ", ".join(
        f"{i['amount']}ml {i['ingredient']}" for i in r.get("ingredients", [])
    )

    return f"""
Recipe name: {r.get('name', '')}
Method: {r.get('method', '')}
Serve: {r.get('serve', '')}
Ingredients: {ingredients_text}
""".strip()


def ingest(file_path: str):
    import json
    from .embeddings import embed
    from .vector_store import get_collection
    logger.info(f"Starting ingestion from file: {file_path}")

    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Extract the recipes list depending on JSON shape
    if isinstance(data, dict):
        if "cocktails" in data:
            recipes = data["cocktails"]
        else:
            recipes = [data]  # fallback single recipe dict
    elif isinstance(data, list):
        recipes = data
    else:
        raise ValueError("Unsupported JSON format")

    collection = get_collection()

    count = 0
    for idx, recipe in enumerate(recipes):  # <-- iterate over 'recipes', not 'data'
        if not isinstance(recipe, dict):
            print(f"Skipping invalid entry at index {idx}")
            continue

        text = recipe_to_text(recipe)
        embedding = embed(text)

        collection.add(
            documents=[text],
            embeddings=[embedding],
            ids=[f"recipe-{idx}"],
            metadatas=[{
                "name": recipe.get("name", ""),
                "method": recipe.get("method", ""),
                "serve": recipe.get("serve", "")
            }]
        )
        count += 1

    print(f"Ingested {count} recipes into ChromaDB")
