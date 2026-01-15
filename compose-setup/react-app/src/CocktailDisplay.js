import * as React from 'react';
import Box from "@mui/material/Box";
import CocktailImage from "./sex-on-the-beach-default.jpg";
import { useCocktail } from "../src/context/CocktailProvider";
import CircularProgress from '@mui/material/CircularProgress';
import {Typography} from "@mui/material";

export default function CocktailDisplay() {
    const { llmResponse, loading, error} = useCocktail();
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
            <Box
                xs={{
                    width: "50%",
                }}
            >
                { loading ? (
                    <Box sx={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        flexDirection: "column",
                        my: 5
                    }}>
                        <Typography
                            variant="subtitle1"
                            fontWeight="bold"
                            textAlign="center"
                            sx={{
                                marginTop: 3,
                            }}
                        >
                            Loading ...
                        </Typography>
                        <Typography
                            variant="body1"
                            fontWeight="bold"
                            textAlign="center"
                            sx={{
                                marginBottom: 3,
                            }}
                        >
                            Loading can take up to 1 minute, relax
                        </Typography>
                        <CircularProgress
                            size={80}
                        />
                    </Box>
                ):(
                    <img
                        style={{ width: "50%", display: "block", margin: "auto" }}
                        src={CocktailImage}
                        alt="Cocktail"
                    />
                ) }
            </Box>
            { llmResponse && (
                <div>
                    <u>Here is your cocktail:</u>
                    <div
                        className="llm-response"
                        dangerouslySetInnerHTML={{ __html: llmResponse }}
                    />
                </div>
            )}
            { error && (<h3>An Error occured!</h3>)}

        </Box>
    );
}