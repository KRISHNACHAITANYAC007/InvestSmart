import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import './styles/ui.css'
import { AuthProvider } from './context/AuthContext.jsx'
import AppErrorBoundary from './components/AppErrorBoundary.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <AppErrorBoundary>
            <AuthProvider>
                <App />
            </AuthProvider>
        </AppErrorBoundary>
    </StrictMode>,
)
