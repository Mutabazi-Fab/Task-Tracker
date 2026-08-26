import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { queryClient } from './app/queryClient'
import { AppRoutes } from './app/routes'
import { ThemeProvider } from './features/theme/ThemeProvider'
import { AppShell } from './components/layout/AppShell'

/** Router + QueryClientProvider only — no layout or feature code of its own. */
function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ThemeProvider>
          <AppShell>
            <AppRoutes />
          </AppShell>
        </ThemeProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
