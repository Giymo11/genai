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

    const [cocktailName, setCocktailName] = useState("No cocktail searched yet");
    const [cocktailDescription, setCocktailDescription] = useState("No cocktail searched yet");

    const clearError = () => setError(null);

    const searchCocktail = async () => {
        setLoading(true);
        setError(null);
        setTimeout(() => {
            console.log("Test for testing loading spinner");
            setLoading(false);
        }, 3000);

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

        try {
            const result = await fetchHello();

            // Keep previous name/description until the next fetch succeeds (your requirement).
            setCocktailName(result?.name ?? "");
            setCocktailDescription(result?.description ?? "");
            console.log(result);
            console.log(result.message);
            console.log(result.status);
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


        setCocktailName("Whiskey Sour");
        setCocktailDescription("Lorem ipsum");

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

            // actions (make methods public)
            setSelectedCategories,
            setInputTextField,
            setLoading,
            searchCocktail,
            clearError,
        }),
        [
            availableCategories,
            selectedCategories,
            inputTextField,
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