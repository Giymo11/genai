// src/context/CocktailProvider.js

import React, { createContext, useContext, useMemo, useState } from "react";
import { fetchCocktailByCategories } from "../api/cocktails";

const CocktailContext = createContext(null);

export function CocktailProvider({ children }) {
    // Hardcoded for now, as requested
    const [possibleCategories] = useState(["Sweet", "Bitter", "Sour", "Comfy", "Modern", "Boozy", "Light", "Fruity"]);

    const [selectedCategories, setSelectedCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null); // user-facing message

    const [cocktailName, setCocktailName] = useState("No cocktail searched yet");
    const [cocktailDescription, setCocktailDescription] = useState("No cocktail searched yet");

    const clearError = () => setError(null);

    const searchCocktail = async () => {
        setLoading(true);
        setError(null);

        /*try {
            const result = await fetchCocktailByCategories(selectedCategories);

            // Keep previous name/description until the next fetch succeeds (your requirement).
            setCocktailName(result?.name ?? "");
            setCocktailDescription(result?.description ?? "");
        } catch (e) {
            // Keep previous cocktailName/cocktailDescription on error (also matches your requirement).
            const message =
                typeof e?.message === "string" && e.message.trim().length > 0
                    ? e.message
                    : "Something went wrong while fetching a cocktail.";
            setError(message);
        } finally {
            setLoading(false);
        }*/
        setCocktailName("Test1");
        setCocktailDescription("Test2");
        setLoading(false);
    };

    const value = useMemo(
        () => ({
            possibleCategories,
            selectedCategories,
            loading,
            error,
            cocktailName,
            cocktailDescription,

            // actions
            setSelectedCategories,
            searchCocktail,
            clearError,
        }),
        [
            possibleCategories,
            selectedCategories,
            loading,
            error,
            cocktailName,
            cocktailDescription,
        ]
    );

    return (
        <CocktailContext.Provider value={value}>
            {children}
        </CocktailContext.Provider>
    );
}

export function useCocktail() {
    const ctx = useContext(CocktailContext);
    if (!ctx) {
        throw new Error("useCocktail must be used within a CocktailProvider");
    }
    return ctx;
}