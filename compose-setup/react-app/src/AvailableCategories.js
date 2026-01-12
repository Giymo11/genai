import * as React from 'react';
import { styled } from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import AddCircle from '@mui/icons-material/AddCircle';
import CheckCircle from '@mui/icons-material/CheckCircle';
import { useCocktail } from "./context/CocktailProvider";
import Box from "@mui/material/Box";
import {Typography} from "@mui/material";

const ListItem = styled('li')(({ theme }) => ({
    margin: theme.spacing(0.5),
}));

export default function AvailableCategories() {
    const { selectedCategories, setSelectedCategories } = useCocktail();
    const { availableCategories } = useCocktail();

    const addSelectedCategory = (categoryToAdd) => () => {
        console.log("Adding category: ", categoryToAdd);
        setSelectedCategories(prev => {// adding is not as simple as with array
            const next = new Set(prev);
            next.add(categoryToAdd);
            return next;
        });
    }

    return (
        <Box>
            <Typography
                variant="body2"
                fontWeight="bold"
                textAlign="center"
            >
                Available categories:
            </Typography>
            <Paper
                sx={{
                    display: 'flex',
                    justifyContent: 'center',
                    flexWrap: 'wrap',
                    listStyle: 'none',
                    p: 0.5,
                    m: 0,
                    bgcolor: 'transparent',
                    boxShadow: 'none',
                }}
                component="ul"
            >
                {Array.from(availableCategories).map((category) => {
                    let icon = <AddCircle />;
                    let color = "default";
                    let variant = "default";


                    if (selectedCategories.has(category)) {
                        icon = <CheckCircle />;
                        color = "success";
                        variant = "outlined";
                    }

                    return (
                        <ListItem key={category}>
                            <Chip
                                icon={icon}
                                label={category}
                                onClick={addSelectedCategory(category)}
                                color={color}
                                variant={variant}
                            />
                        </ListItem>
                    );
                })}
            </Paper>
        </Box>
    );
}
