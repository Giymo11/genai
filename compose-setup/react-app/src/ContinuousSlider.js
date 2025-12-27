import * as React from 'react';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Slider from '@mui/material/Slider';
import VolumeDown from '@mui/icons-material/VolumeDown';
import VolumeUp from '@mui/icons-material/VolumeUp';

export default function ContinuousSlider() {
    const [value, setValue] = React.useState(30);

    const handleChange = (event, newValue) => {
        setValue(newValue);
    };

    return (
        <Box sx={{ width: 200 }}>
            <Stack spacing={2} direction="row" sx={{ alignItems: 'center', mb: 1 }}>
                <VolumeDown />
                <Slider aria-label="Volume" value={value} onChange={handleChange} />
                <VolumeUp />
            </Stack>
            <Stack spacing={2} direction="row" sx={{ alignItems: 'center', mb: 1 }}>
                <Slider aria-label="Volume" value={value} onChange={handleChange} />
            </Stack>
        </Box>
    );
}
