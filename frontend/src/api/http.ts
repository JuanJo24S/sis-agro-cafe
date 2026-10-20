import { leerToken, notificarSesionExpirada } from '../lib/sesion'

export type Servicio = 'auth' | 'agronomico' | 'cosecha' | 'financiero' | 'gateway'

export interface ErrorCampo {
  campo: string
  mensaje: string
}

/** Error uniforme para REST (problem+json, RFC 9457) y GraphQL. */
export class ApiError extends Error {
  readonly estado: number
  readonly errores: ErrorCampo[]

  constructor(mensaje: string, estado = 0, errores: ErrorCampo[] = []) {
    super(mensaje)
    this.name = 'ApiError'
    this.estado = estado
    this.errores = errores
  }
}

const MENSAJE_SIN_CONEXION =
  'No fue posible comunicarse con el servidor. Verifica que los microservicios estén arriba (docker compose up).'

/** Llama a un microservicio a traves del proxy /ms/<servicio> (Vite en desarrollo, nginx en Docker). */
export async function rest<T>(
  servicio: Servicio,
  ruta: string,
  opciones: { metodo?: string; cuerpo?: unknown; cabeceras?: Record<string, string>; anonimo?: boolean } = {},
): Promise<T> {
  const cabeceras: Record<string, string> = { Accept: 'application/json', ...opciones.cabeceras }
  if (opciones.cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
  const token = leerToken()
  if (token && !opciones.anonimo) cabeceras.Authorization = `Bearer ${token}`

  let respuesta: Response
  try {
    respuesta = await fetch(`/ms/${servicio}${ruta}`, {
      method: opciones.metodo ?? 'GET',
      headers: cabeceras,
      body: opciones.cuerpo === undefined ? undefined : JSON.stringify(opciones.cuerpo),
    })
  } catch {
    throw new ApiError(MENSAJE_SIN_CONEXION)
  }

  if (respuesta.status === 401 && !opciones.anonimo) {
    notificarSesionExpirada()
    throw new ApiError('Tu sesión expiró. Inicia sesión de nuevo.', 401)
  }
  if (!respuesta.ok) throw await errorDe(respuesta)
  if (respuesta.status === 204) return undefined as T
  const texto = await respuesta.text()
  return (texto ? JSON.parse(texto) : undefined) as T
}

async function errorDe(respuesta: Response): Promise<ApiError> {
  if (respuesta.status === 502 || respuesta.status === 503 || respuesta.status === 504) {
    return new ApiError('El microservicio no está disponible en este momento.', respuesta.status)
  }
  try {
    const problema = await respuesta.json()
    return new ApiError(
      problema.detail ?? problema.title ?? `Error ${respuesta.status}`,
      respuesta.status,
      Array.isArray(problema.errores) ? problema.errores : [],
    )
  } catch {
    return new ApiError(`Error ${respuesta.status} del servidor.`, respuesta.status)
  }
}

export { MENSAJE_SIN_CONEXION }
