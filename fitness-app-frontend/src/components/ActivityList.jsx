import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Button, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,
} from '@mui/material';
import { formatDate, getActivityDate, isRecent } from '../utils/stats';

// Shows the history table. The activities come from the parent (ActivitiesPage).
const ActivityList = ({ activities, calorieMap = {} }) => {
  const navigate = useNavigate();

  if (activities.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <Typography variant="h6">No activities yet</Typography>
        <Typography color="text.secondary">Add your first workout above to see it here.</Typography>
      </Box>
    );
  }

  // Newest first
  const sorted = [...activities].sort((a, b) => (getActivityDate(b) || 0) - (getActivityDate(a) || 0));

  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Activity</TableCell>
            <TableCell align="right">Time (min)</TableCell>
            <TableCell align="right">Calories (est.)</TableCell>
            <TableCell>Date</TableCell>
            <TableCell />
          </TableRow>
        </TableHead>
        <TableBody>
          {sorted.map((a) => (
            <TableRow key={a.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/activities/${a.id}`)}>
              <TableCell>
                <Chip label={a.type} color="primary" variant="outlined" size="small" />
              </TableCell>
              <TableCell align="right">{a.duration}</TableCell>
              <TableCell align="right">
                {calorieMap[a.id] != null
                  ? `${calorieMap[a.id]} kcal`
                  : (!(a.id in calorieMap) && isRecent(a) ? 'Calculating…' : '—')}
              </TableCell>
              <TableCell>{formatDate(a)}</TableCell>
              <TableCell align="right">
                <Button size="small" onClick={(e) => { e.stopPropagation(); navigate(`/activities/${a.id}`); }}>
                  AI insights
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default ActivityList;
