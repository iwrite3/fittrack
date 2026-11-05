// Pure helper functions: no React, no API calls. Easy to read and to test.

// An activity's date is its startTime if set, otherwise the createdAt time added by the backend.
export const getActivityDate = (activity) => {
  const raw = activity.startTime || activity.createdAt;
  if (!raw) return null;
  const date = new Date(raw);
  return isNaN(date) ? null : date;
};

// Midnight at the start of the given day.
const startOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate());

// True if the activity was logged in the last few minutes (the AI may still be working on it).
export const isRecent = (activity, minutes = 5) => {
  const date = getActivityDate(activity);
  return !!date && Date.now() - date.getTime() < minutes * 60 * 1000;
};

// Totals for the last `days` days, counting today as day 1.
// days = 1 -> today, days = 7 -> last 7 days, days = 30 -> last month.
// calorieMap = { activityId: kcal } built from the AI recommendations.
export const summarize = (activities, days, calorieMap = {}) => {
  const from = startOfDay(new Date());
  from.setDate(from.getDate() - (days - 1));

  let calories = 0;
  let minutes = 0;
  let workouts = 0;

  activities.forEach((activity) => {
    const date = getActivityDate(activity);
    if (date && date >= from) {
      calories += calorieMap[activity.id] || 0;
      minutes += activity.duration || 0;
      workouts += 1;
    }
  });

  return { calories, minutes, workouts };
};

export const formatDate = (activity) => {
  const date = getActivityDate(activity);
  return date ? date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—';
};
