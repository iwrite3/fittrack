import React, { useState, useContext, useEffect } from 'react';
import { AuthContext } from 'react-oauth2-code-pkce';
import { useDispatch } from 'react-redux';
import { setCredentials, logout } from './store/authSlice';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import { Alert, AppBar, Box, Button, Card, CircularProgress, Container, Toolbar, Typography } from '@mui/material';
import ActivityForm from './components/ActivityForm';
import ActivityList from './components/ActivityList';
import ActivityDetail from './components/ActivityDetail';
import StatsCards from './components/StatsCards';
import { getActivities, getUserRecommendations } from './services/api';
import { isRecent } from './utils/stats';

const ActivitiesPage = () => {
  const [activities, setActivities] = useState([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const [loadError, setLoadError] = useState(null);
  // { activityId: kcal } taken from the AI recommendations
  const [calorieMap, setCalorieMap] = useState({});

  // Load the user's activities on first render and again whenever a new one is added
  useEffect(() => {
    getActivities()
      .then((res) => { setActivities(res.data || []); setLoadError(null); })
      .catch((error) => {
        console.error('Error fetching activities:', error);
        const status = error.response ? error.response.status : 'no response';
        setLoadError(`Could not load your history (${status}). Check that the gateway and services are running.`);
      });
  }, [refreshKey]);

  // The AI writes calories a few seconds after the save, so ask again until every recent activity has one
  useEffect(() => {
    if (activities.length === 0) return undefined;
    const userId = localStorage.getItem('userId');
    let cancelled = false;
    let timer;
    let tries = 0;
    const load = async () => {
      try {
        const res = await getUserRecommendations(userId);
        if (cancelled) return;
        const map = {};
        (res.data || []).forEach((r) => { map[r.activityId] = r.caloriesBurned ?? null; });
        setCalorieMap(map);
        tries += 1;
        const waiting = activities.some((a) => !(a.id in map) && isRecent(a));
        if (waiting && tries < 20) timer = setTimeout(load, 4000);
      } catch (error) {
        console.error('Error fetching recommendations:', error);
      }
    };
    load();
    return () => { cancelled = true; clearTimeout(timer); };
  }, [activities]);

  return (
    <Box sx={{ display: 'grid', gap: 3 }}>
      {loadError && <Alert severity="error">{loadError}</Alert>}
      <StatsCards activities={activities} calorieMap={calorieMap} />
      <Card sx={{ p: 3 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>Log a workout</Typography>
        <ActivityForm onActivityAdded={() => setRefreshKey((k) => k + 1)} />
      </Card>
      <Card sx={{ p: 3 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>History</Typography>
        <ActivityList activities={activities} calorieMap={calorieMap} />
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
