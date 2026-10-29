import React from 'react'
import ReactDOM from 'react-dom/client'
import { authConfig } from './authConfig'
import { Provider } from 'react-redux'
import { store } from './store/store'
import App from './App'
import { AuthProvider } from 'react-oauth2-code-pkce'
import { ThemeProvider } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import theme from './theme'
import './index.css'

const root = ReactDOM.createRoot(document.getElementById('root'))
root.render(
  <AuthProvider authConfig={authConfig}>
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <App />
      </ThemeProvider>
    </Provider>
  </AuthProvider>
)
