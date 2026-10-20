import { useState, type FormEvent } from 'react'
import { KeyRound, UserRound } from 'lucide-react'
import { mutaciones, usePerfil } from '../api/agrocafe'
import { BotonEnviar, Campo, Cargando, Dato, Encabezado, ErrorCarga, ErrorFormulario, Insignia } from '../components/ui'
import { useAuth } from '../lib/auth'
import { ROLES, fecha, iniciales } from '../lib/formato'
import { errorDeCampo, useAccion, useFormulario } from '../lib/useAccion'
import type { Usuario } from '../api/types'

export function Perfil() {
  const perfil = usePerfil()

  return (
    <>
      <Encabezado titulo="Mi perfil" descripcion="Datos de tu cuenta en ms-auth." />
      {perfil.isLoading ? (
        <div className="tarjeta">
          <Cargando />
        </div>
      ) : !perfil.data ? (
        <div className="tarjeta">
          <ErrorCarga error={perfil.error} reintentar={() => perfil.refetch()} />
        </div>
      ) : (
        <div className="rejilla-perfil">
          <div className="tarjeta perfil">
            <span className="avatar avatar--grande" aria-hidden>
              {iniciales(perfil.data.nombre)}
            </span>
            <h2>{perfil.data.nombre}</h2>
            <p className="texto-secundario">{perfil.data.email}</p>
            <div className="etiquetas">
              <Insignia tono="cafe">{ROLES[perfil.data.rol]}</Insignia>
              <Insignia tono={perfil.data.estado === 'ACTIVO' ? 'exito' : 'neutro'}>{perfil.data.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</Insignia>
            </div>
            <dl className="datos datos--una">
              <Dato etiqueta="Miembro desde" valor={fecha(perfil.data.fechaCreacion)} />
              <Dato etiqueta="Identificador" valor={<code>{perfil.data.id.slice(0, 8)}…</code>} />
            </dl>
          </div>
          <div className="pila">
            <FormularioNombre usuario={perfil.data} />
            <FormularioClave />
          </div>
        </div>
      )}
    </>
  )
}

function FormularioNombre({ usuario }: { usuario: Usuario }) {
  const { actualizarNombre } = useAuth()
  const [nombre, setNombre] = useState(usuario.nombre)
  const accion = useAccion(mutaciones.actualizarPerfil, {
    exito: 'Perfil actualizado',
    alTerminar: (u) => actualizarNombre(u.nombre),
  })
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar(nombre.trim())
  }
  return (
    <form className="tarjeta" onSubmit={enviar}>
      <div className="tarjeta__cabecera">
        <h2>
          <UserRound size={18} aria-hidden /> Datos personales
        </h2>
      </div>
      <ErrorFormulario mensaje={accion.error} />
      <Campo etiqueta="Nombre completo" error={errorDeCampo(accion.erroresCampo, 'nombre')}>
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={120} />
      </Campo>
      <div className="acciones-form">
        <BotonEnviar ocupado={accion.ocupado}>Guardar</BotonEnviar>
      </div>
    </form>
  )
}

function FormularioClave() {
  const f = useFormulario({ actual: '', nueva: '', confirmacion: '' })
  const [error, setError] = useState<string | null>(null)
  const accion = useAccion(mutaciones.cambiarPassword, {
    exito: 'Contraseña actualizada',
    alTerminar: () => f.reiniciar({ actual: '', nueva: '', confirmacion: '' }),
  })
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    if (f.valores.nueva !== f.valores.confirmacion) {
      setError('La confirmación no coincide con la nueva contraseña.')
      return
    }
    setError(null)
    accion.ejecutar(f.valores.actual, f.valores.nueva)
  }
  return (
    <form className="tarjeta" onSubmit={enviar}>
      <div className="tarjeta__cabecera">
        <h2>
          <KeyRound size={18} aria-hidden /> Cambiar contraseña
        </h2>
      </div>
      <ErrorFormulario mensaje={error ?? accion.error} />
      <div className="rejilla-form">
        <Campo etiqueta="Contraseña actual" ancho="completo">
          <input {...f.enlazar('actual')} type="password" required autoComplete="current-password" />
        </Campo>
        <Campo etiqueta="Nueva contraseña" ayuda="Mínimo 8 caracteres">
          <input {...f.enlazar('nueva')} type="password" required minLength={8} maxLength={100} autoComplete="new-password" />
        </Campo>
        <Campo etiqueta="Confirmar nueva contraseña">
          <input {...f.enlazar('confirmacion')} type="password" required minLength={8} maxLength={100} autoComplete="new-password" />
        </Campo>
      </div>
      <div className="acciones-form">
        <BotonEnviar ocupado={accion.ocupado}>Actualizar contraseña</BotonEnviar>
      </div>
    </form>
  )
}
