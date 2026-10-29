import React, { useState } from 'react';
import { Box, Button, FormControl, InputLabel, MenuItem, Select, TextField } from '@mui/material';
import { addActivity } from '../services/api';

const empty = { type: 'RUNNING', duration: '', caloriesBurned: '' };

const ActivityForm = ({ onActivityAdded }) => {
  const [activity, setActivity] = useState(empty);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await addActivity({ ...activity, additionalMetrics: {} });
      setActivity(empty);
      onActivityAdded();
    } catch (error) {
      console.error(error);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit}
      sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr 1fr' }, alignItems: 'center' }}>
      <FormControl fullWidth>
        <InputLabel id="activity-type">Activity</InputLabel>
        <Select labelId="activity-type" label="Activity" value={activity.type}
          onChange={(e) => setActivity({ ...activity, type: e.target.value })}>
          <MenuItem value="WALKING">Walking</MenuItem>
          <MenuItem value="RUNNING">Running</MenuItem>
          <MenuItem value="CYCLING">Cycling</MenuItem>
          <MenuItem value="SWIMMING">Swimming</MenuItem>
        </Select>
      </FormControl>
      <TextField label="Time (minutes)" type="number" value={activity.duration} required
        slotProps={{ htmlInput: { min: 1 } }}
        onChange={(e) => setActivity({ ...activity, duration: e.target.value })} />
      <TextField label="Calories burned" type="number" value={activity.caloriesBurned} required
        slotProps={{ htmlInput: { min: 0 } }}
        onChange={(e) => setActivity({ ...activity, caloriesBurned: e.target.value })} />
      <Box sx={{ gridColumn: { sm: '1 / -1' } }}>
        <Button type="submit" variant="contained" size="large" disabled={saving}>
          {saving ? 'Saving...' : 'Add activity'}
        </Button>
      </Box>
    </Box>
  );
};

export default ActivityForm;
