import * as React from 'react';
import { styled } from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import AddCircle from '@mui/icons-material/AddCircle';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircle from '@mui/icons-material/CheckCircle';
import { useCocktail } from "./context/CocktailProvider";
import Box from "@mui/material/Box";

const ListItem = styled('li')(({ theme }) => ({
    margin: theme.spacing(0.5),
}));

export default function SelectedCategories() {
    const { selectedCategories, setSelectedCategories } = useCocktail();

    const handleDelete = (categoryToDelete) => () => {
        console.log("Deleting category: ", categoryToDelete);
        const updatedSelectedCategories = selectedCategories.filter((category) => category !== categoryToDelete);
        setSelectedCategories(updatedSelectedCategories);
        // setCategory((chips) => categories.filter((category) => category.key !== categoryToDelete.key));
    };

    return (
        <Box>
            <h2>Selected categories</h2>
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
                {selectedCategories.map((category) => {
                    /*let icon = <AddCircle />;
                    let color = "default";
                    let variant = "default";


                    if (selectedCategories.includes(data.label)) {
                        icon = <CheckCircle />;
                        color = "success";
                        variant = "outlined";
                    }*/

                    return (
                        <ListItem key={category}>
                            <Chip
                                // icon={icon}
                                label={category}
                                // onClick={addSelectedCategory(data.label)}
                                color="default"
                                onDelete={handleDelete(category)}
                            />
                        </ListItem>
                    );
                })}
            </Paper>
        </Box>
    );
}
