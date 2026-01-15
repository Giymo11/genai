/**
 * Fetch a cocktail based on selected categories.
 *   { query: string, tags: string[] }
 */
export async function fetchCocktailByCategories(inputTextField, selectedCategories = []) {
    const url = "http://localhost:5005/recommend_cocktail";
    const body = JSON.stringify({query:inputTextField, tags: selectedCategories });
    try {
        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: body,
        });

        if (!res.ok) {
            throw new Error("Could not fetch a cocktail. Please try again.");
        }

        const data = await res.json();
        return data;
    } catch (error) {
        console.error(error);
        return {
            name: data?.name ?? "An error occured",
            description: data?.description ?? "Error",
        };
    }
}

export async function fetchHello() {
    const res = await fetch("http://localhost:5005/hello");

    if (!res.ok) {
        throw new Error("Failed to fetch hello message");
    }

    return await res.json();
}