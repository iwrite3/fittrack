import React from 'react';
import { Box, Card, Typography } from '@mui/material';
import { summarize } from '../utils/stats';

// The three periods we show. `days` is passed to summarize().
const PERIODS = [
  { label: 'Today', days: 1 },
  { label: 'Last 7 days', days: 7 },
  { label: 'Last month', days: 30 },
];

const StatsCards = ({ activities }) => (
  <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' } }}>
    {PERIODS.map((period, index) => {
      const { calories, minutes, workouts } = summarize(activities, period.days);
      const highlighted = index === 0; // make the first card (Today) stand out
      return (
        <Card
          key={period.label}
          sx={{
            p: 3,
            color: highlighted ? '#fff' : 'text.primary',
            background: highlighted ? 'linear-gradient(135deg, #e0457b, #ff7aa8)' : '#fff',
          }}
        >
          <Typography variant="body2" sx={{ opacity: 0.85 }}>{period.label}</Typography>
          <Typography variant="h4" sx={{ my: 0.5, color: highlighted ? '#fff' : 'primary.main' }}>
            {calories} <Typography component="span" variant="body1">kcal</Typography>
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.85 }}>
            {workouts} {workouts === 1 ? 'workout' : 'workouts'} · {minutes} min
          </Typography>
        </Card>
      );
    })}
  </Box>
);

export default StatsCards;
