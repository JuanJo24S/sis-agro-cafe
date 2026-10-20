import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { gql } from '../api/graphql'
import type { Rol, Token, Usuario } from '../api/types'
import {
  EVENTO_SESION_EXPIRADA,
  decodificar,
  guardarToken,
  leerToken,
  vigente,
  type Claims,
} from './sesion'

interface ContextoAuth {
  usuario: Claims | null
  iniciarSesion: (email: string, password: string) => Promise<void>
  registrarse: (datos: { nombre: string; email: string; password: string; rol: Rol }) => Promise<void>
  cerrarSesion: (motivo?: string) => void
  /** Mensaje para la pantalla de login (p. ej. "tu sesion expiro"). */
  aviso: string | null
  actualizarNombre: (nombre: string) => void
}

const Contexto = createContext<ContextoAuth | null>(null)

const LOGIN = /* GraphQL */ `
  mutation Login($input: LoginInput!) {
    login(input: $input) { accessToken tokenType expiresIn }
  }
`
const REGISTRO = /* GraphQL */ `
  mutation Registro($input: RegistroInput!) {
    registro(input: $input) { id nombre email rol estado }
  }
`

function claimsIniciales(): Claims | null {
  const token = leerToken()
  const claims = token ? decodificar(token) : null
  if (!vigente(claims)) {
    guardarToken(null)
    return null
  }
  return claims
}

export function ProveedorAuth({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Claims | null>(claimsIniciales)
  const [aviso, setAviso] = useState<string | null>(null)
  const cliente = useQueryClient()

  const cerrarSesion = useCallback(
    (motivo?: string) => {
      guardarToken(null)
      setUsuario(null)
      setAviso(motivo ?? null)
      cliente.clear()
    },
    [cliente],
  )

  // Cierre automatico al vencer el JWT o cuando un microservicio responde 401.
  useEffect(() => {
    const alExpirar = () => cerrarSesion('Tu sesión expiró. Inicia sesión de nuevo.')
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
    let temporizador: number | undefined
    if (usuario) {
      const restante = usuario.exp * 1000 - Date.now()
      temporizador = window.setTimeout(alExpirar, Math.max(restante, 0))
    }
    return () => {
      window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
      window.clearTimeout(temporizador)
    }
  }, [usuario, cerrarSesion])

  const iniciarSesion = useCallback(async (email: string, password: string) => {
    const { login } = await gql<{ login: Token }>(LOGIN, { input: { email, password } })
    const claims = decodificar(login.accessToken)
    if (!claims) throw new Error('El servidor devolvió un token inválido.')
    guardarToken(login.accessToken)
    setAviso(null)
    setUsuario(claims)
  }, [])

  const registrarse = useCallback<ContextoAuth['registrarse']>(
    async (datos) => {
      await gql<{ registro: Usuario }>(REGISTRO, { input: datos })
      await iniciarSesion(datos.email, datos.password)
    },
    [iniciarSesion],
  )

  const actualizarNombre = useCallback((nombre: string) => {
    setUsuario((actual) => (actual ? { ...actual, nombre } : actual))
  }, [])

  const valor = useMemo(
    () => ({ usuario, iniciarSesion, registrarse, cerrarSesion, aviso, actualizarNombre }),
    [usuario, iniciarSesion, registrarse, cerrarSesion, aviso, actualizarNombre],
  )
  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useAuth(): ContextoAuth {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useAuth debe usarse dentro de ProveedorAuth')
  return contexto
}
