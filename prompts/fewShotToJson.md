## Instructions:
You are a precise data analyst. Parse the following TSV data into the JSON format defined below. The data may contain zero, one, or multiple cocktail recipes, as well as additional information. 

### The Challenge
The TSV data is messy. It often lists two different cocktails side-by-side in visual columns. 
- You must distinguish between these columns if present.
- You must identify the "Header" (Cocktail Name) and the cells below it (Ingredients and Amounts).
- Ingredients always follow the format "<amount> <unit (optional)> <ingredient>.

### Conversion Rules
1. **Volume:** Convert ALL measurements to integers in milliliters (ml).
   - 1 dash = 1 ml
   - 1 barspoon = 5 ml
   - 1 oz = 30 ml
   - 0.75 oz (or 22.5ml) = 23 ml (Always round .5 up)
   - 0.5 oz = 15 ml
2. **Ratings:** If a rating exists (e.g., "5.5", "5+*"), extract it as a string. If none, use "unrated".
3. **Method/Serve:** Look for keywords like "stir", "shake", "up", "rocks", "crushed".
   - instructions like "stir", "shake", "blend" go into `method`.
   - instructions like "up", "rocks", "pebbled", "neat", "crushed" go into `serve`.
4. **Notes/Garnish:** If an ingredient has no quantity (e.g., "mint spray" or "lemon peel"), put it in garnish. Preparation notes, author, date, source, taste notes, history, variations goes in notes.

### Example 1
```text
Old Fashioned		
stir	60	bourbon/rye
rocks	8	demerara syrup
	2	ango
	1	orange bitters
```
- Found Old Fashioned in column 1. Unrated, method is stir, serve is rocks. Ingredients: 60 bourbon/rye, 8 demerara syrup, 2 ango, 1 orange bitters. No notes.

### Example 2
```text
Kings County						
stir	45 ml	Highland single malt scotch whisky				
up	10 ml	Torabhaig Peated Single Malt Whisky		ciro's special		
	10 ml	Luxardo Maraschino liqueur		shake	50	dark rum
	10 ml	Punt E Mes vermouth amaro			20	mure cassis
					20	lime juice
Big Chief					5	dry orange curacao
stir	45 ml	Bourbon whiskey				
up	10 ml	Amaro (e.g. Averna)		Summertime Punch (original)		
	10 ml	Punt E Mes vermouth amaro		crushed	60	myers
					30	lime
Lakeshore		4.5 (5 with braulio)			15	rich syrup
stir	60 ml	Havana Club 3 Year Old rum			45	water
up	30 ml	Dubonnet, Byrrh etc. rouge light quinquina			2	ango
	7.5 ml	Fernet Branca liqueur			1	grapefruit
	7.5 ml	Luxardo Maraschino liqueur				
```

- Found "Kings County" in column 1. Method: "stir", Serve: "up". Ingredients: 45 Highland malt, 10 Torabhaig, 10 Luxardo, 10 Punt E Mes. (there are ingredients in column 2 from another cocktail that has been cut off in this example)
- Found "ciro's special" in column 2. Method: "shake". Ingredients: 50 dark rum, 20 mure cassis, 20 lime juice, 5 dry orange curacao.
- Found "Big Chief" in column 1. Method: "stir", Serve: "up". Ingredients: 45 Bourbon, 10 Amaro, 10 Punt E Mes.
- Found "Summertime Punch" in column 2. Serve: "crushed". Note: "original". Ingredients: 60 myers, 30 lime, 15 rich syrup, 45 water, 2 ango, 1 grapefruit.
- Found "Lakeshore" in column 1. Rating: "4.5". Method: "stir", Serve: "up". Math: 7.5ml becomes 8. Note: "5 with braulio".


## Format Definition:
```json
{
  "cocktail_specs": [
    {
      "name": "string",
      "rating": "string",
      "serve": "string",
      "method": "string",
      "ingredients": [
        { "name": "string", "volume_ml": number }
      ],
      "garnish": "string",
      "notes": ["string"]
    }
  ],
  "other_data": {
    "description": "string",
    "data": ["string"]
  }
}
```

## Guidelines:
Respond ONLY with the valid JSON using the keys cocktail_specs and other_data. No markdown code blocks, no explanation. Use the "other_data" field only if there is non-cocktail-recipe information.

## Data (TSV):
$content