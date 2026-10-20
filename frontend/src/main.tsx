import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '@fontsource-variable/plus-jakarta-sans'
import '@fontsource-variable/fraunces'
import './styles.css'
import { App } from './App'
import { ProveedorAvisos } from './components/avisos'
import { ProveedorAuth } from './lib/auth'
import { ApiError } from './api/http'

const cliente = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      refetchOnWindowFocus: false,
      // Un 4xx no mejora reintentando; un microservicio que arranca si.
      retry: (intentos, error) => !(error instanceof ApiError && error.estado >= 400 && error.estado < 500) && intentos < 1,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={cliente}>
      <BrowserRouter>
        <ProveedorAvisos>
          <ProveedorAuth>
            <App />
          </ProveedorAuth>
        </ProveedorAvisos>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
