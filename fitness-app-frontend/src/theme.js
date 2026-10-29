import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#e0457b', light: '#ff7aa8', dark: '#a8305a', contrastText: '#fff' },
    secondary: { main: '#f8bbd0' },
    background: { default: '#fff5f8', paper: '#ffffff' },
    text: { primary: '#4a1d33', secondary: '#8a5a70' },
  },
  shape: { borderRadius: 16 },
  typography: {
    fontFamily: '"Poppins", "Segoe UI", Roboto, system-ui, sans-serif',
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  components: {
    MuiButton: { styleOverrides: { root: { borderRadius: 999, paddingInline: 22 } } },
    MuiCard: {
      styleOverrides: {
        root: { boxShadow: '0 8px 30px rgba(224, 69, 123, 0.12)', border: '1px solid #fde1ea' },
      },
    },
    MuiTableCell: {
      styleOverrides: { head: { fontWeight: 700, color: '#a8305a', backgroundColor: '#fde1ea' } },
    },
  },
});

export default theme;
