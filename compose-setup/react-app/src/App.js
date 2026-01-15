import React from 'react';
import { Box } from "@mui/material";
import AvailableCategories from "./AvailableCategories";
import ChatBox from "./ChatBox";
import { CocktailProvider } from "./context/CocktailProvider";
import CocktailDisplay from "./CocktailDisplay";
import SelectedCategories from "./SelectedCategories";
import SendRequestButton from "./SendRequestButton";

function App() {
  return (
    <CocktailProvider>
        <Box
            sx={{
                display: "flex",
                height: "100vh", // full viewport height
            }}
        >
            <Box
                sx={{
                    height: "90vh",
                    width: "90vw",
                    margin: "auto",

                    // styling
                    bgcolor: "#F5F5F5",
                    borderRadius: 3,
                    border: "1px solid #B0BEC5",// blue grey: https://materialui.co/colors
                    display: "flex",
                }}
            >
                {/* Left Section */}
                <Box
                    sx={{
                        width: "50%",
                        display: "flex",
                        flexDirection: "column",
                        justifyContent: "space-evenly",
                        p: 2,
                        borderRight: "1px solid ##B0BEC5",// blue grey: https://materialui.co/colors
                    }}
                >
                    <Box
                        sx={{
                            textAlign: "center",
                        }}
                    >
                        <h1>
                            Welcome to the smartes cockt-AI-l mixer
                        </h1>
                        <h2>Add your wishes below and get excited about your Cocktail</h2>
                    </Box>
                    <Box>
                        <AvailableCategories />
                        <ChatBox />
                        <SelectedCategories />
                        <SendRequestButton />
                    </Box>
                </Box>


                {/* Right Section */}
                <Box
                    sx={{
                        width: "50%",
                        p: 2,
                    }}
                >
                    <CocktailDisplay />
                </Box>
            </Box>
        </Box>
    </CocktailProvider>
  );
}

export default App;
