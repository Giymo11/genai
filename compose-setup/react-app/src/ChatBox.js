import * as React from 'react';
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import { useCocktail } from "../src/context/CocktailProvider";

export default function ChatBox() {
    const { inputTextField, setInputTextField } = useCocktail();

    const handleInputChange = (event) => {
        const newInput = event.target.value;
        setInputTextField(newInput);
    };

    return (
        <Box
            sx={{
                p: 0.5,
                m: 0,
                width: "85%",
                marginLeft: "auto",
                marginRight: "auto",
                marginTop: 6,
                display: "flex",
                flexDirection: "column",
                justifyContent: "flex-end",
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center" }}>
                <TextField
                    fullWidth
                    multiline
                    minRows={3}
                    maxRows={7}
                    placeholder="Type a message..."
                    value={inputTextField}
                    onChange={handleInputChange}
                    variant="outlined"
                />
            </Box>
        </Box>
    );
}
