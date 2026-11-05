import React, { useState } from 'react';
import { Alert, Box, Button, Snackbar, TextField } from '@mui/material';
import { addActivity } from '../services/api';

const empty = { type: '', duration: '' };

const ActivityForm = ({ onActivityAdded }) => {
  const [activity, setActivity] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null); // { severity, text }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await addActivity({ ...activity, type: activity.type.trim(), additionalMetrics: {} });
      setActivity(empty);
      // The save is instant. The AI insights are generated in the background.
      setMessage({ severity: 'success', text: 'Activity saved! AI insights are being generated in the background.' });
      onActivityAdded();
    } catch (error) {
      console.error(error);
      setMessage({ severity: 'error', text: 'Could not save the activity. Please try again.' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Box component="form" onSubmit={handleSubmit}
        sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '2fr 1fr' }, alignItems: 'center' }}>
        <TextField label="Activity" placeholder="e.g. Morning jog, Yoga, Football" value={activity.type} required
          onChange={(e) => setActivity({ ...activity, type: e.target.value })} />
        <TextField label="Time (minutes)" type="number" value={activity.duration} required
          slotProps={{ htmlInput: { min: 1 } }}
          onChange={(e) => setActivity({ ...activity, duration: e.target.value })} />
        <Box sx={{ gridColumn: { sm: '1 / -1' } }}>
          <Button type="submit" variant="contained" size="large" disabled={saving}>
            {saving ? 'Saving...' : 'Add activity'}
          </Button>
        </Box>
      </Box>
      <Snackbar open={!!message} autoHideDuration={5000} onClose={() => setMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        {message ? <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>{message.text}</Alert> : undefined}
      </Snackbar>
    </>
  );
};

export default ActivityForm;
