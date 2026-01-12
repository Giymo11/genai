import React, { createContext, useContext, useMemo, useState } from "react";
import { fetchCocktailByCategories } from "../api/cocktails";

const CocktailContext = createContext(null);

export function CocktailProvider({ children }) {
    const [availableCategories] = useState(// no setter needed
        () => new Set(["Sweet", "Bitter", "Sour", "Comfy", "Modern", "Boozy", "Light", "Fruity"])
    );

    const [selectedCategories, setSelectedCategories] = useState(() => new Set());
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
            availableCategories,
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
            availableCategories,
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