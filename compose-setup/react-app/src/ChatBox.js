import * as React from 'react';
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import IconButton from "@mui/material/IconButton";
import SendIcon from "@mui/icons-material/Send";

export default function ChatBox() {
    const [userInput, setUserInput] = React.useState("");

    const handleInputChange = (event) => {
        setUserInput(event.target.value);
    };

    const handleSend = () => {
        console.log("User input:", userInput);
        setUserInput(""); // optional: clear input after send
    };

    return (
        <Box
            sx={{
                p: 0.5,
                m: 0,
                width: "75%",
                marginLeft: "auto",
                marginRight: "auto",
                marginTop: 6,
                display: "flex",
                flexDirection: "column",
                justifyContent: "flex-end",
            }}
        >
            {/* Input area */}
            <Box sx={{ display: "flex", alignItems: "center" }}>
                <TextField
                    fullWidth
                    multiline
                    minRows={3}
                    maxRows={7}
                    placeholder="Type a message..."
                    value={userInput}
                    onChange={handleInputChange}
                    variant="outlined"
                />

                <IconButton
                    onClick={handleSend}
                    sx={{ ml: 1 }}
                    color="primary"
                >
                    <SendIcon />
                </IconButton>
            </Box>
        </Box>
    );
}
