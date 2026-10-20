// Tipos del contrato (openapi/gateway.graphqls). Las fechas viajan como String ISO-8601.

export type Rol = 'CAFICULTOR' | 'ANALISTA_FINANCIERO'
export type EstadoUsuario = 'ACTIVO' | 'INACTIVO'
export type TipoLabor = 'LIMPIA' | 'FUMIGACION' | 'DESHIERBE' | 'PODA' | 'OTRA'
export type EstadoFertilizacion = 'PROGRAMADA' | 'EJECUTADA' | 'CANCELADA'
export type EstadoGrano = 'CEREZA' | 'MOJADO' | 'SECO_AGUA' | 'SECO_TRILLA' | 'ALMACENADO'

export interface Usuario {
  id: string
  nombre: string
  email: string
  rol: Rol
  estado: EstadoUsuario
  fechaCreacion?: string
}

export interface Token {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface Balance {
  loteId?: string | null
  fincaId?: string | null
  costoTotal: number
  ingresoTotal: number
  balance: number
  actualizadoEn?: string | null
}

export interface Finca {
  id: string
  propietarioId?: string
  nombre: string
  municipio: string
  departamento?: string | null
  areaTotalHectareas?: number | null
  fechaCreacion?: string
}

export interface Lote {
  id: string
  fincaId: string
  nombre: string
  variedad: string
  fechaSiembra: string
  areaHectareas: number
  densidadSiembra?: number | null
  alturaMsnm?: number | null
  phSuelo?: number | null
  tipoSuelo?: string | null
  temperaturaPromedio?: number | null
}

export interface Labor {
  id: string
  loteId: string
  tipo: TipoLabor
  descripcion?: string | null
  fechaEjecucion: string
  jornales?: number | null
  costoManoObra: number
  costoInsumos?: number | null
  costoTotal: number
  fechaRegistro?: string
}

export interface Fertilizacion {
  id: string
  loteId: string
  estado: EstadoFertilizacion
  fecha: string
  composicion: string
  dosisKgPorArbol?: number | null
  cantidadTotalKg?: number | null
  costoInsumos?: number | null
  costoManoObra?: number | null
  costoTotal: number
  observaciones?: string | null
  fechaRegistro?: string
}

export interface Recoleccion {
  id: string
  loteId: string
  fecha: string
  pesoCascaraKg: number
  valorPorKgArroba: number
  recolector?: string | null
  estadoGrano: EstadoGrano
  costoManoObra: number
  fechaRegistro?: string
}

export interface Venta {
  id: string
  loteId: string
  fecha: string
  cantidad: number
  precioPorArroba: number
  comprador: string
  montoTotal: number
  fechaRegistro?: string
}

export interface Pagina<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
