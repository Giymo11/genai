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
                            Welcome to the smartes cocktail mixer
                        </h1>
                        <h2>Add your wishes below and get excited about your Cockt<b>AI</b>l</h2>
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

/* Notes from assignment
The system will be provided as a web application, offering:
A clean and simple ingredient input interface.
A search bar supporting natural language queries.
We consider a web application to be the most useful for our users,
since it is the most accessible in various situations and for different
workflows. For instance, a larger screen could show the app in a home
office setting where the user prepares for a cocktail session, a tablet
running the web application could be installed behind the counter in a
bar to prepare cocktails. In addition, even a phone could be used on the
go if no other larger screen is available.
Another aspect for choosing a web application lies in its simplicity for
creating applications with existing tools/frameworks. Web applications are
independent of an operating system, provide easy access via every browser as
well as rapid application development.
 */

export default App;
