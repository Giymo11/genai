import * as React from 'react';
import Box from "@mui/material/Box";
import CocktailImage from "./sex-on-the-beach-default.jpg";
import { useCocktail } from "../src/context/CocktailProvider"

export default function CocktailDisplay() {
    const { cocktailName, cocktailDescription, selectedCategories } = useCocktail();
    return (
        <Box
            sx={{
                display: "flex",
                flexDirection: "column",
                p: 4,
                m: 2,
                border: "1px solid #B0BEC5",
                borderRadius: 3,
                height: "calc(100% - 92px)",//added margin
                alignContent: "center",
                alignItems: "center",
                justifyContent: "space-evenly",
            }}
        >
            <Box>
                <h2>Whiskey Sour TEST-{ cocktailName }</h2>
                <h3>{ cocktailDescription }</h3>
            </Box>
            <Box
                xs={{
                    width: "75%",
                }}
            >
                <img
                    style={{ width: "75%", display: "block", margin: "auto" }}
                    src={CocktailImage}
                    alt="Cocktail"
                />
            </Box>
            <Box>//info: ingredients usually: 2-8; check rest_api.pyt
                <b>Ingredients:</b>
                <ul>
                    <li>60 oz whiskey</li>
                    <li>20g sugar</li>
                    <li>20g sugar</li>
                    <li>20g sugar</li>
                    <li>20g sugar</li>
                </ul>
                more:
                <ol>
                    { selectedCategories.map((category) => <li>{ category }</li>) }
                </ol>
            </Box>
        </Box>
    );
}