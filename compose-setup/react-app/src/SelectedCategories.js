import * as React from 'react';
import { styled } from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import DeleteIcon from '@mui/icons-material/Delete';
import { useCocktail } from "./context/CocktailProvider";
import Box from "@mui/material/Box";
import {Typography} from "@mui/material";

const ListItem = styled('li')(({ theme }) => ({
    margin: theme.spacing(0.5),
}));

export default function SelectedCategories() {
    const { selectedCategories, setSelectedCategories } = useCocktail();

    const handleDelete = (categoryToDelete) => () => {
        console.log("Deleting category: ", categoryToDelete);
        setSelectedCategories(prev => {// deleting is not as simple as with array
            const next = new Set(prev); // clonging set, important!
            next.delete(categoryToDelete);
            return next;
        });
    };

    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
            }}
        >
            <Typography
                variant="body2"
                fontWeight="bold"
                textAlign="center"
            >
                { selectedCategories.size > 0 ? "Selected categories:" : "No selected categories yet" }
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
                {Array.from(selectedCategories).map((category) => {// creating an array from set for iteration
                    return (
                        <ListItem key={category}>
                            <Chip
                                label={category}
                                color="primary"
                                onDelete={handleDelete(category)}
                                deleteIcon={<DeleteIcon />}
                            />
                        </ListItem>
                    );
                })}
            </Paper>
        </Box>
    );
}
