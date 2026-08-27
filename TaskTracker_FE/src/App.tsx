import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { queryClient } from './app/queryClient'
import { AppRoutes } from './app/routes'
import { ThemeProvider } from './features/theme/ThemeProvider'
import { AuthProvider } from './features/auth/AuthContext'

/**
 * Router + QueryClientProvider + AuthProvider only — no layout or feature code of its
 * own. AppShell (sidebar + top bar) is no longer wrapped here: /login and /signup render
 * without it, so each protected route wraps itself individually (see routes.tsx).
 */
function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ThemeProvider>
          <AuthProvider>
            <AppRoutes />
          </AuthProvider>
        </ThemeProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
