import type { EstadoFertilizacion, EstadoGrano, Rol, TipoLabor } from '../api/types'

const moneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})
const monedaCompacta = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  notation: 'compact',
  maximumFractionDigits: 1,
})
const numero = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })

export const pesos = (v?: number | null) => moneda.format(v ?? 0)
export const pesosCortos = (v?: number | null) => monedaCompacta.format(v ?? 0)
export const num = (v?: number | null) => (v == null ? '—' : numero.format(v))

/** Las fechas LocalDate ("2026-03-14") se leen como fecha local, no UTC. */
function aFecha(valor: string): Date {
  return /^\d{4}-\d{2}-\d{2}$/.test(valor) ? new Date(`${valor}T00:00:00`) : new Date(valor)
}

export function fecha(valor?: string | null): string {
  if (!valor) return '—'
  return aFecha(valor).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function fechaHora(valor?: string | null): string {
  if (!valor) return '—'
  return aFecha(valor).toLocaleString('es-CO', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function hoyISO(desplazamientoDias = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + desplazamientoDias)
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

/** Edad del cultivo a partir de la fecha de siembra: "3 años 2 meses". */
export function edadCultivo(fechaSiembra: string): string {
  const inicio = aFecha(fechaSiembra)
  const hoy = new Date()
  let meses = (hoy.getFullYear() - inicio.getFullYear()) * 12 + (hoy.getMonth() - inicio.getMonth())
  if (hoy.getDate() < inicio.getDate()) meses--
  if (meses < 0) return 'Por sembrar'
  if (meses < 1) return 'Recién sembrado'
  const anios = Math.floor(meses / 12)
  const resto = meses % 12
  const partes = []
  if (anios) partes.push(`${anios} ${anios === 1 ? 'año' : 'años'}`)
  if (resto) partes.push(`${resto} ${resto === 1 ? 'mes' : 'meses'}`)
  return partes.join(' ')
}

export const ROLES: Record<Rol, string> = {
  CAFICULTOR: 'Caficultor',
  ANALISTA_FINANCIERO: 'Analista financiero',
}

export const TIPOS_LABOR: Record<TipoLabor, string> = {
  LIMPIA: 'Limpia',
  FUMIGACION: 'Fumigación',
  DESHIERBE: 'Deshierbe',
  PODA: 'Poda',
  OTRA: 'Otra',
}

export const ESTADOS_FERTILIZACION: Record<EstadoFertilizacion, string> = {
  PROGRAMADA: 'Programada',
  EJECUTADA: 'Ejecutada',
  CANCELADA: 'Cancelada',
}

/** Trazabilidad post-cosecha; el grano solo avanza en este orden (EstadoGrano.java). */
export const ORDEN_GRANO: EstadoGrano[] = ['CEREZA', 'MOJADO', 'SECO_AGUA', 'SECO_TRILLA', 'ALMACENADO']

export const ESTADOS_GRANO: Record<EstadoGrano, string> = {
  CEREZA: 'Cereza',
  MOJADO: 'Mojado',
  SECO_AGUA: 'Seco de agua',
  SECO_TRILLA: 'Seco de trilla',
  ALMACENADO: 'Almacenado',
}

export const iniciales = (nombre: string) =>
  nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('')
