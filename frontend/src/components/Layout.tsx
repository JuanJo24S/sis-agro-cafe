import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { Activity, ChartColumn, HandCoins, LayoutDashboard, LogOut, Menu, Trees, X } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { ROLES, iniciales } from '../lib/formato'

const NAVEGACION = [
  { a: '/', etiqueta: 'Panel', icono: LayoutDashboard, fin: true },
  { a: '/fincas', etiqueta: 'Fincas y lotes', icono: Trees },
  { a: '/ventas', etiqueta: 'Ventas', icono: HandCoins },
  { a: '/finanzas', etiqueta: 'Finanzas', icono: ChartColumn },
  { a: '/sistema', etiqueta: 'Estado del sistema', icono: Activity },
]

export function Layout() {
  const { usuario, cerrarSesion } = useAuth()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const { pathname } = useLocation()

  useEffect(() => {
    setMenuAbierto(false)
    window.scrollTo({ top: 0 })
  }, [pathname])

  return (
    <div className={`app ${menuAbierto ? 'app--menu' : ''}`}>
      <aside className="lateral" aria-label="Navegación principal">
        <div className="lateral__marca">
          <img src="/logo-agrocafe-icon-light.png" alt="" width={40} height={40} />
          <div>
            <strong>AgroCafé</strong>
            <span>Gestión cafetera</span>
          </div>
          <button className="icono-btn lateral__cerrar" onClick={() => setMenuAbierto(false)} aria-label="Cerrar menú">
            <X size={20} />
          </button>
        </div>

        <nav className="lateral__nav">
          {NAVEGACION.map(({ a, etiqueta, icono: Icono, fin }) => (
            <NavLink key={a} to={a} end={fin} className={({ isActive }) => `nav-item ${isActive ? 'nav-item--activo' : ''}`}>
              <Icono size={19} aria-hidden />
              <span>{etiqueta}</span>
            </NavLink>
          ))}
        </nav>

        {usuario && (
          <div className="lateral__usuario">
            <NavLink to="/perfil" className="usuario-tarjeta" title="Mi perfil">
              <span className="avatar" aria-hidden>
                {iniciales(usuario.nombre)}
              </span>
              <span className="usuario-tarjeta__texto">
                <strong>{usuario.nombre}</strong>
                <span>{ROLES[usuario.rol] ?? usuario.rol}</span>
              </span>
            </NavLink>
            <button className="icono-btn icono-btn--claro" onClick={() => cerrarSesion()} title="Cerrar sesión" aria-label="Cerrar sesión">
              <LogOut size={18} />
            </button>
          </div>
        )}
      </aside>

      <div className="velo" onClick={() => setMenuAbierto(false)} aria-hidden />

      <div className="principal">
        <header className="barra-movil">
          <button className="icono-btn" onClick={() => setMenuAbierto(true)} aria-label="Abrir menú">
            <Menu size={22} />
          </button>
          <img src="/logo-agrocafe-icon.png" alt="" width={30} height={30} />
          <strong>AgroCafé</strong>
        </header>
        <main className="contenido">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
