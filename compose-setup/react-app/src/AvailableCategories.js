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

export default function AvailableCategories() {
    const { selectedCategories, setSelectedCategories } = useCocktail();
    const [chipData, setChipData] = React.useState([
        { key: 0, label: 'Sweet' },
        { key: 1, label: 'Bitter' },
        { key: 2, label: 'Sour' },
        { key: 3, label: 'Comfy' },
        { key: 4, label: 'Modern' },
        { key: 5, label: 'Boozy' },
        { key: 6, label: 'Light' },
        { key: 7, label: 'Fruity' },
    ]);

    const handleDelete = (categoryToDelete) => () => {
        console.log("Deleting category: ", categoryToDelete);
        // setCategory((chips) => categories.filter((category) => category.key !== categoryToDelete.key));
    };

    const addSelectedCategory = (category) => () => {
        console.log("Adding category: ", category);
        const updatedSelectedCategories = [...selectedCategories, category];
        setSelectedCategories(updatedSelectedCategories);
        console.log("Does it contain fruity? ", updatedSelectedCategories.includes("Fruity"));
        console.log("Updated selected categories: ", updatedSelectedCategories);
    }

    return (
        <Box>
            <h2>Available categories</h2>
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
                {chipData.map((data) => {
                    let icon = <AddCircle />;
                    let color = "default";
                    let variant = "default";
                    let clickable = true;


                    if (selectedCategories.includes(data.label)) {
                        icon = <CheckCircle />;
                        color = "success";
                        variant = "outlined";
                        clickable = false;
                    }

                    return (
                        <ListItem key={data.key}>
                            <Chip
                                icon={icon}
                                label={data.label}
                                onClick={addSelectedCategory(data.label)}
                                color={color}
                                variant={variant}
                                clickable={clickable}
                            />
                        </ListItem>
                    );
                })}
            </Paper>
        </Box>
    );
}
