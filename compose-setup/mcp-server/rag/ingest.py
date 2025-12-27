from .embeddings import embed
from .vector_store import add_recipe
import json

def recipe_to_text(r):
    return f"""
Title: {r['title']}
Ingredients: {', '.join(r['ingredients'])}
Instructions: {r['instructions']}
"""

def ingest(file_path):
    with open(file_path) as f:
        recipes = json.load(f)

    for r in recipes:
        text = recipe_to_text(r)
        add_recipe(
            recipe_id=r["id"],
            text=text,
            embedding=embed(text),
            metadata={
                "dietary": r.get("dietary", []),
                "cooking_time": r.get("cooking_time", 0)
            }
        )
    print(f"Ingested {len(recipes)} recipes")
