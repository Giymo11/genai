import * as React from 'react';
import Box from "@mui/material/Box";
import SendIcon from "@mui/icons-material/Send";
import Button from '@mui/material/Button';
import { useCocktail } from "../src/context/CocktailProvider";

export default function SendRequestButton() {
    const { loading, searchCocktail } = useCocktail();

    const handleSend = () => {
        searchCocktail();
    };

    return (
        <Box
            sx={{
                p: 0.5,
                m: 0,
                width: "50%",
                marginLeft: "auto",
                marginRight: "auto",
                marginTop: 6,
                display: "flex",
                flexDirection: "column",
                justifyContent: "flex-end",
            }}
        >
            <Button
                variant="contained"
                endIcon={<SendIcon />}
                onClick={handleSend}
                loading={loading}
                loadingPosition="end"//needed so that button text is displayed during loading
            >
                Generate cocktail
            </Button>
        </Box>
    );
}
