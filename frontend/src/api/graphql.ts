import { leerToken, notificarSesionExpirada } from '../lib/sesion'
import { ApiError, MENSAJE_SIN_CONEXION } from './http'

interface ErrorGraphQL {
  message: string
  path?: (string | number)[]
  extensions?: { classification?: string }
}

const ESTADO_POR_CLASIFICACION: Record<string, number> = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
}

/**
 * Cliente del Gateway GraphQL (BFF). El gateway no valida el JWT: lo propaga a cada
 * microservicio, que si lo valida. Si la respuesta trae datos parciales (p. ej. un
 * microservicio caido resolviendo un campo opcional) se devuelven los datos.
 */
export async function gql<T>(consulta: string, variables?: Record<string, unknown>): Promise<T> {
  const cabeceras: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  }
  const token = leerToken()
  if (token) cabeceras.Authorization = `Bearer ${token}`

  let respuesta: Response
  try {
    respuesta = await fetch('/graphql', {
      method: 'POST',
      headers: cabeceras,
      body: JSON.stringify({ query: consulta, variables }),
    })
  } catch {
    throw new ApiError(MENSAJE_SIN_CONEXION)
  }
  if (respuesta.status >= 502) {
    throw new ApiError('El Gateway GraphQL no está disponible en este momento.', respuesta.status)
  }

  let cuerpo: { data?: T | null; errors?: ErrorGraphQL[] }
  try {
    cuerpo = await respuesta.json()
  } catch {
    throw new ApiError(`Respuesta inválida del gateway (HTTP ${respuesta.status}).`, respuesta.status)
  }

  const errores = cuerpo.errors ?? []
  if (token && errores.some((e) => e.extensions?.classification === 'UNAUTHORIZED')) {
    notificarSesionExpirada()
    throw new ApiError('Tu sesión expiró. Inicia sesión de nuevo.', 401)
  }
  if (errores.length > 0 && !cuerpo.data) {
    const primero = errores[0]
    const estado = ESTADO_POR_CLASIFICACION[primero.extensions?.classification ?? ''] ?? 500
    const mensaje = /INTERNAL_ERROR/i.test(primero.message)
      ? 'Ocurrió un error interno en el servidor.'
      : primero.message
    throw new ApiError(mensaje, estado)
  }
  if (errores.length > 0) console.warn('[GraphQL] respuesta parcial', errores)
  return cuerpo.data as T
}
