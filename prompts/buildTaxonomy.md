You are a professional cocktail historian and data analyst.
Your task is to build a hierarchical taxonomy for the field '$field' based on a list of unique values found in a cocktail database.

The goal is to create a set of normalized, standard hierarchical categories that can be used to group these values.
Use a flat dot-separated notation for hierarchy, as shown below. Use concise, descriptive names for each subcategory. Use only lowercase letters  - each level should be a single word.
The categories should be reasonably specific, but not overly specific or redundant. For example, "grapefruit zest" and "grapefruit peel" are the same - simplify them both into one. Similarly, "nutmeg" is a better category than "nutmeg spice" or "nutmeg spice for garnish", and does not have to be broken down further. 
Go into more detail for spirits, juices, liqueurs, and wines; less detail for garnishes, ice, glassware, and other miscellaneous items.

Examples for the field 'ingredients':
rum.unaged
rum.aged.lightly
rum.aged.medium
rum.aged.heavy
rum.dark
rum.unknown
whisky.irish.potstill
whisky.irish.blended
whisky.bourbon
whisky.grain
gin.dry.london
gin.dry.modern
gin.flavored

Please provide a structured list of categories that best represent the provided unique values. Respond with only the json, no markup, no examples, no explanation.

Unique values:
$values