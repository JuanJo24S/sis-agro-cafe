import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { useAuth } from './lib/auth'
import { Login } from './pages/Login'
import { Panel } from './pages/Panel'
import { Fincas } from './pages/Fincas'
import { FincaDetalle } from './pages/FincaDetalle'
import { LoteDetalle } from './pages/LoteDetalle'
import { Ventas } from './pages/Ventas'
import { Finanzas } from './pages/Finanzas'
import { Perfil } from './pages/Perfil'
import { Sistema } from './pages/Sistema'

export function App() {
  const { usuario } = useAuth()

  if (!usuario) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Panel />} />
        <Route path="fincas" element={<Fincas />} />
        <Route path="fincas/:fincaId" element={<FincaDetalle />} />
        <Route path="lotes/:loteId" element={<LoteDetalle />} />
        <Route path="ventas" element={<Ventas />} />
        <Route path="finanzas" element={<Finanzas />} />
        <Route path="perfil" element={<Perfil />} />
        <Route path="sistema" element={<Sistema />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
