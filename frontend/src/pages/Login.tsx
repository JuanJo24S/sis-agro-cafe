import { useState, type FormEvent } from 'react'
import { ChartColumn, Eye, EyeOff, HandCoins, Info, Leaf, Loader2, Sprout, WandSparkles } from 'lucide-react'
import type { Rol } from '../api/types'
import { Campo, ErrorFormulario } from '../components/ui'
import { useAuth } from '../lib/auth'
import { mensajeDe, useFormulario } from '../lib/useAccion'

const CUENTA_DEMO = {
  nombre: 'Caficultor Demo',
  email: 'demo@agrocafe.co',
  password: 'AgroCafe2026',
  rol: 'CAFICULTOR' as Rol,
}

const CARACTERISTICAS = [
  { icono: Sprout, titulo: 'Fincas y lotes', texto: 'Caracteriza cada parcela: variedad, altura, suelo y edad del cultivo.' },
  { icono: Leaf, titulo: 'Labores y abonadas', texto: 'Bitácora de labores culturales y plan de fertilización.' },
  { icono: HandCoins, titulo: 'Cosecha y ventas', texto: 'Trazabilidad del grano desde la cereza hasta la venta.' },
  { icono: ChartColumn, titulo: 'Balance por lote', texto: 'Costos e ingresos consolidados en tiempo real con eventos.' },
]

export function Login() {
  const { iniciarSesion, registrarse, aviso } = useAuth()
  const [modo, setModo] = useState<'ingresar' | 'registrar'>('ingresar')
  const [ocupado, setOcupado] = useState<'form' | 'demo' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [verClave, setVerClave] = useState(false)
  const f = useFormulario({ nombre: '', email: '', password: '', rol: 'CAFICULTOR' })

  const ejecutar = async (tipo: 'form' | 'demo', tarea: () => Promise<void>) => {
    setOcupado(tipo)
    setError(null)
    try {
      await tarea()
    } catch (e) {
      setError(mensajeDe(e))
      setOcupado(null)
    }
  }

  const enviar = (e: FormEvent) => {
    e.preventDefault()
    const { nombre, email, password, rol } = f.valores
    void ejecutar('form', () =>
      modo === 'ingresar' ? iniciarSesion(email, password) : registrarse({ nombre, email, password, rol: rol as Rol }),
    )
  }

  // Para la presentacion: entra con la cuenta demo y, si aun no existe, la crea.
  const entrarDemo = () =>
    ejecutar('demo', async () => {
      try {
        await iniciarSesion(CUENTA_DEMO.email, CUENTA_DEMO.password)
      } catch {
        await registrarse(CUENTA_DEMO)
      }
    })

  const cambiarModo = (nuevo: typeof modo) => {
    setModo(nuevo)
    setError(null)
  }

  return (
    <div className="acceso">
      <section className="acceso__hero">
        <div className="acceso__hero-contenido">
          <div className="acceso__sello">
            <img src="/logo-agrocafe-icon-light.png" alt="" width={44} height={44} />
            <span>AgroCafé</span>
          </div>
          <h1>
            Del cafetal a las cuentas claras, <em>lote por lote.</em>
          </h1>
          <p>Sistema de gestión agronómica y financiera para caficultores.</p>
          <ul className="acceso__caracteristicas">
            {CARACTERISTICAS.map(({ icono: Icono, titulo, texto }) => (
              <li key={titulo}>
                <span>
                  <Icono size={18} aria-hidden />
                </span>
                <div>
                  <strong>{titulo}</strong>
                  <p>{texto}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
        <p className="acceso__pie">Programación Avanzada · FET Neiva</p>
      </section>

      <section className="acceso__panel">
        <div className="acceso__tarjeta">
          <img className="acceso__logo" src="/logo-agrocafe-full.png" alt="AgroCafé" width={190} />

          <div className="segmentado" role="tablist">
            <button role="tab" aria-selected={modo === 'ingresar'} className={modo === 'ingresar' ? 'activo' : ''} onClick={() => cambiarModo('ingresar')}>
              Iniciar sesión
            </button>
            <button role="tab" aria-selected={modo === 'registrar'} className={modo === 'registrar' ? 'activo' : ''} onClick={() => cambiarModo('registrar')}>
              Crear cuenta
            </button>
          </div>

          {aviso && !error && (
            <div className="alerta alerta--info">
              <Info size={18} aria-hidden />
              <span>{aviso}</span>
            </div>
          )}
          <ErrorFormulario mensaje={error} />

          <form onSubmit={enviar} className="acceso__form">
            {modo === 'registrar' && (
              <Campo etiqueta="Nombre completo">
                <input {...f.enlazar('nombre')} required maxLength={120} autoComplete="name" placeholder="Ej. María Fernanda Rojas" />
              </Campo>
            )}
            <Campo etiqueta="Correo electrónico">
              <input {...f.enlazar('email')} type="email" required maxLength={180} autoComplete="email" placeholder="tu@correo.com" />
            </Campo>
            <Campo etiqueta="Contraseña" ayuda={modo === 'registrar' ? 'Mínimo 8 caracteres' : undefined}>
              <div className="entrada-icono">
                <input
                  {...f.enlazar('password')}
                  type={verClave ? 'text' : 'password'}
                  required
                  minLength={modo === 'registrar' ? 8 : undefined}
                  maxLength={100}
                  autoComplete={modo === 'registrar' ? 'new-password' : 'current-password'}
                />
                <button type="button" className="icono-btn" onClick={() => setVerClave((v) => !v)} aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}>
                  {verClave ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </Campo>
            {modo === 'registrar' && (
              <Campo etiqueta="Perfil">
                <select {...f.enlazar('rol')}>
                  <option value="CAFICULTOR">Caficultor</option>
                  <option value="ANALISTA_FINANCIERO">Analista financiero</option>
                </select>
              </Campo>
            )}
            <button type="submit" className="btn btn--primario btn--bloque" disabled={!!ocupado}>
              {ocupado === 'form' && <Loader2 size={16} className="girar" />}
              {modo === 'ingresar' ? 'Ingresar' : 'Crear cuenta e ingresar'}
            </button>
          </form>

          <div className="separador">
            <span>o</span>
          </div>
          <button className="btn btn--secundario btn--bloque" onClick={entrarDemo} disabled={!!ocupado}>
            {ocupado === 'demo' ? <Loader2 size={16} className="girar" /> : <WandSparkles size={16} />}
            Entrar con la cuenta demo
          </button>
          <p className="acceso__nota">
            {CUENTA_DEMO.email} · se crea automáticamente si no existe
          </p>
        </div>
      </section>
    </div>
  )
}
