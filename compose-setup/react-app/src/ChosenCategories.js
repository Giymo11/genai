import * as React from 'react';
import { styled } from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import Paper from '@mui/material/Paper';
import AddCircle from '@mui/icons-material/AddCircle';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import DeleteIcon from '@mui/icons-material/Delete';

const ListItem = styled('li')(({ theme }) => ({
    margin: theme.spacing(0.5),
}));

export default function ChosenCategories() {
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
        setCategory((chips) => categories.filter((category) => category.key !== categoryToDelete.key));
    };

    return (
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

                if (data.label === 'Fruity') {
                    icon = <AddCircleOutlineIcon />;
                }

                return (
                    <ListItem key={data.key}>
                        <Chip
                            icon={icon}
                            label={data.label}
                            onClick={() => alert(`Add ${data.label}!`)}
                            onDelete={data.label === 'Fruity' ? undefined : handleDelete(data)}
                            deleteIcon={<DeleteIcon />}
                        />
                    </ListItem>
                );
            })}
        </Paper>
    );
}
