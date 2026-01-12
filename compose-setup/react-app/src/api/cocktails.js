// src/api/cocktails.js

/**
 * Fetch a cocktail based on selected categories.
 *
 * Replace the URL + response parsing once you wire up the real API.
 * Return shape should stay:
 *   { name: string, description: string }
 */
export async function fetchCocktailByCategories(selectedCategories = []) {
    // TODO: Replace with your real endpoint
    const url = "/api/cocktails/search";

    // Example fetch (adjust method/headers/body to your API needs)
    const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ categories: selectedCategories }),
    });

    if (!res.ok) {
        // Try to surface a useful message, but keep it user-facing
        throw new Error("Could not fetch a cocktail. Please try again.");
    }

    // TODO: Adjust parsing to your API's real response shape
    const data = await res.json();

    // Placeholder mapping (update later)
    return {
        name: data?.name ?? "Unknown cocktail",
        description: data?.description ?? "No description available.",
    };
}

export async function fetchHello() {
    const res = await fetch("http://localhost:5005/hello");

    if (!res.ok) {
        throw new Error("Failed to fetch hello message");
    }

    return await res.json();
}