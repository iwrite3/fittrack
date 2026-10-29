import React, { useState, useContext, useEffect } from 'react';
import { AuthContext } from 'react-oauth2-code-pkce';
import { useDispatch } from 'react-redux';
import { setCredentials, logout } from './store/authSlice';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import { AppBar, Box, Button, Card, CircularProgress, Container, Toolbar, Typography } from '@mui/material';
import ActivityForm from './components/ActivityForm';
import ActivityList from './components/ActivityList';
import ActivityDetail from './components/ActivityDetail';
import StatsCards from './components/StatsCards';
import { getActivities } from './services/api';

const ActivitiesPage = () => {
  const [activities, setActivities] = useState([]);
  const [refreshKey, setRefreshKey] = useState(0);

  // Load the user's activities on first render and again whenever a new one is added
  useEffect(() => {
    getActivities()
      .then((res) => setActivities(res.data || []))
      .catch((error) => console.error('Error fetching activities:', error));
  }, [refreshKey]);

  return (
    <Box sx={{ display: 'grid', gap: 3 }}>
      <StatsCards activities={activities} />
      <Card sx={{ p: 3 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>Log a workout</Typography>
        <ActivityForm onActivityAdded={() => setRefreshKey((k) => k + 1)} />
      </Card>
      <Card sx={{ p: 3 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>History</Typography>
        <ActivityList activities={activities} />
      </Card>
    </Box>
  );
};

function App() {
  const { token, tokenData, logIn, logOut, error } = useContext(AuthContext);
  const dispatch = useDispatch();
  const [ready, setReady] = useState(false);

  // Save the token to localStorage BEFORE the pages below start calling the API
  useEffect(() => {
    if (token && tokenData) {
      dispatch(setCredentials({ token, user: tokenData }));
      setReady(true);
    } else {
      setReady(false);
    }
  }, [token, tokenData, dispatch]);

  const handleLogout = () => {
    dispatch(logout());
    logOut();
  };

  if (!token) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 2 }}>
        <Card sx={{ p: { xs: 4, sm: 6 }, textAlign: 'center', maxWidth: 420, width: '100%' }}>
          <Typography variant="h3" sx={{ fontWeight: 700, color: 'primary.main' }}>FitTrack</Typography>
          <Typography color="text.secondary" sx={{ mt: 1, mb: 4 }}>
            Log your workouts and get AI coaching tips.
          </Typography>
          {error && <Typography color="error" sx={{ mb: 2 }}>Login error: {error}</Typography>}
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Button variant="contained" size="large" onClick={() => logIn()}>Login</Button>
            <Button variant="outlined" size="large" onClick={() => logIn(undefined, { prompt: 'create' })}>
              Sign up
            </Button>
          </Box>
        </Card>
      </Box>
    );
  }

  const displayName = tokenData?.given_name || tokenData?.preferred_username || tokenData?.email || '';

  return (
    <Router>
      <AppBar position="sticky" elevation={0}
        sx={{ background: 'linear-gradient(90deg, #e0457b, #ff7aa8)' }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>FitTrack</Typography>
          {displayName && (
            <Typography sx={{ mr: 2, display: { xs: 'none', sm: 'block' } }}>Hi, {displayName}</Typography>
          )}
          <Button color="inherit" variant="outlined" onClick={handleLogout}
            sx={{ borderColor: 'rgba(255,255,255,0.7)' }}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="md" sx={{ py: 4 }}>
        {!ready ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 6 }}><CircularProgress /></Box>
        ) : (
        <Routes>
          <Route path="/activities" element={<ActivitiesPage />} />
          <Route path="/activities/:id" element={<ActivityDetail />} />
          <Route path="*" element={<Navigate to="/activities" replace />} />
        </Routes>
        )}
      </Container>
    </Router>
  );
}

export default App;
