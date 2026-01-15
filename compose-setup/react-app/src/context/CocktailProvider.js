import React, { createContext, useContext, useMemo, useState } from "react";
import {fetchCocktailByCategories, fetchHello} from "../api/cocktails";

const CocktailContext = createContext(null);

export function CocktailProvider({ children }) {
    const [availableCategories] = useState(// no setter needed
        () => new Set(["Classic", "Rum", "Whisky", "Tropical", "Fruity", "Bitter", "Non-Alcoholic", "Comfy", "Innovative", "Summer", "Japanese", "Refreshing", "Film-inspired", "Adaptable", "Intense", "Aperitif"])
    );
    const [selectedCategories, setSelectedCategories] = useState(() => new Set());
    const [inputTextField, setInputTextField] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null); // user-facing message
    const [llmResponse, setLlmResponse] = useState("");

    const [cocktailName, setCocktailName] = useState("No cocktail searched yet");
    const [cocktailDescription, setCocktailDescription] = useState("No cocktail searched yet");

    const clearError = () => setError(null);

    const searchCocktail = async () => {
        setLlmResponse(null);
        setLoading(true);
        setError(null);

        try {
            if (inputTextField.length === 0 || inputTextField === " "){
                setInputTextField("No categories selected");
            }
            const result = await fetchCocktailByCategories(inputTextField, Array.from(selectedCategories));
            const rawResponse = result.response;
            const cleanResponse = rawResponse.replaceAll("\n", "");
            setLlmResponse(cleanResponse);
        } catch (e) {
            // Keep previous cocktailName/cocktailDescription on error (also matches your requirement).
            const message =
                typeof e?.message === "string" && e.message.trim().length > 0
                    ? e.message
                    : "Something went wrong while fetching a cocktail.";
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    const value = useMemo(
        () => ({
            availableCategories,
            selectedCategories,
            inputTextField,
            loading,
            error,
            cocktailName,
            cocktailDescription,
            llmResponse,

            // actions (make methods public)
            setSelectedCategories,
            setInputTextField,
            setLoading,
            searchCocktail,
            clearError,
            setLlmResponse,
        }),
        [
            availableCategories,
            selectedCategories,
            inputTextField,
            loading,
            error,
            cocktailName,
            cocktailDescription,
            llmResponse,
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