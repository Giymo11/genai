You are a precise data analyst. Parse the following TSV data into the JSON format defined below.

Format Definition:
{
  "cocktail_specs": [
    {
      "name": "string",
      "rating": "string (up to 4.5 stars, or 'unrated')",
      "serve": "string (e.g. 'rocks', 'up', 'crushed', pebbled', etc. Information about the ice always goes here.)",
      "method": "string (e.g. 'stir', 'shake', 'blend', etc.)",
      "ingredients": [
        {
          "name": "string",
          "volume_ml": "number (integer), convert all measurements to ml: one dash is 0 ml, a barspoon is 5 ml, use 23 instead of 22.5"
        }
      ],
      "garnish": "string (optional, dont include if none)",
      "notes": [
        "string (optional, dont include if none) - e.g. preparation notes, author, date, source, taste notes, history, variations"
      ]
    }
  ],
  "other_data": {
    "description": "string (information about what this csv represents)",
    "data": [
      "string (optional, information in this sheet that is not part of the cocktail specification data, e.g., 'this sheet focuses on rum-based cocktails' or 'data collected from various sources including ...')"
    ]
  }
}

Guidelines:
1. If the data contains a cocktail recipe, extract the name, ingredients, and notes. Do not use the "other_data" field in this case.
2. If the data does not contain a cocktail recipe, describe what the data contains in the "other_data" field. Do not use the "cocktail_specs" field in this case.
3. If the data contains multiple cocktail recipes, include all of them in the "cocktail_specs" array. Use the "other_data" field only if there is additional non-cocktail information.

Ingredients always follow the format "<amount> <unit (optional)> <ingredient>. If there is an amount, write the ingredient and amount into the ingredients list. If there is no amount specified, write the ingredient into the notes or garnish. Do not write ingredients with amounts into the notes or garnish.
If there are multiple ingredients possible as substitutions, write them into the ingredient name. 
If the amount is a range, repeat the cocktail recipe and include both, one with the minimum and one with the maximum amount.
Ensure that each cocktail has at least two ingredients to be considered a valid recipe. Again, if not, use "other_data" instead.

Respond ONLY with the valid JSON. Do not include markdown formatting or explanations

Data (TSV):
$content