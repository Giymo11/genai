import os
import logging
import sys
import json
from fastmcp import FastMCP
from rag.query import search_recipes

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

mcp = FastMCP("MCP Tools")

# @mcp.tool()
# def provide_cocktail_recipe(input: str) -> str:
#     """ Provides a string with a cocktail recipe description """
#     logger.info("=== TOOL CALLED: provide_cocktail_recipe ===")
#     recipe = """🍹 Classic Mojito Recipe
#
# Ingredients:
# - 2 oz (60 ml) white rum
# - 1 oz (30 ml) fresh lime juice
# - 2 teaspoons sugar
# - 6-8 fresh mint leaves
# - Soda water
# - Ice cubes
# - Lime wedge and mint sprig for garnish
#
# Instructions:
# 1. Place mint leaves and sugar in a glass
# 2. Muddle gently to release mint oils (don't over-muddle)
# 3. Add lime juice and stir to dissolve sugar
# 4. Fill glass with ice cubes
# 5. Pour rum over ice
# 6. Top with soda water (about 2-4 oz)
# 7. Stir gently to combine
# 8. Garnish with lime wedge and mint sprig
#
# Tips:
# - Use fresh mint for the best flavor
# - Don't muddle too hard or the mint will become bitter
# - Adjust sugar to taste
# - Can be made virgin by omitting the rum
#
# Enjoy your refreshing Mojito! """
#     return recipe

@mcp.tool()
def search_for_cocktail_recipes_in_db(input: str) -> str:
    """ Provides a string with a cocktail recipe description """
    logger.info("=== TOOL CALLED: search_for_cocktail_recipes_in_db ===")
    result = search_recipes(input, k=5)
    return json.dumps(result)

if __name__ == '__main__':
    host = os.getenv("HOST", "0.0.0.0")
    mcp_port = int(os.getenv("MCP_PORT", 6006))
    mcp.run(transport="streamable-http", host=host, port=mcp_port)