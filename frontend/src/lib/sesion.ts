import type { Rol } from '../api/types'

const CLAVE_TOKEN = 'agrocafe.token'
export const EVENTO_SESION_EXPIRADA = 'agrocafe:sesion-expirada'

/** Claims que emite ms-auth (JwtService): sub, email, nombre, rol, exp. */
export interface Claims {
  sub: string
  email: string
  nombre: string
  rol: Rol
  exp: number
}

export function leerToken(): string | null {
  try {
    return localStorage.getItem(CLAVE_TOKEN)
  } catch {
    return null
  }
}

export function guardarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(CLAVE_TOKEN, token)
    else localStorage.removeItem(CLAVE_TOKEN)
  } catch {
    // Almacenamiento bloqueado (modo privado): la sesion vive solo en memoria.
  }
}

export function decodificar(token: string): Claims | null {
  try {
    const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(carga)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    )
    return JSON.parse(json) as Claims
  } catch {
    return null
  }
}

export function vigente(claims: Claims | null): claims is Claims {
  return !!claims && claims.exp * 1000 > Date.now()
}

export function notificarSesionExpirada() {
  window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA))
}

/** Clave para el encabezado Idempotency-Key (UUID). randomUUID solo existe en contexto seguro. */
export function nuevaClave(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto && window.isSecureContext) {
    return crypto.randomUUID()
  }
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const h = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`
}
