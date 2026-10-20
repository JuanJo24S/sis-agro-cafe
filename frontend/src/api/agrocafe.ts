import { useQuery, type QueryClient } from '@tanstack/react-query'
import { gql } from './graphql'
import { rest } from './http'
import type {
  Balance,
  EstadoGrano,
  Fertilizacion,
  Finca,
  Labor,
  Lote,
  Pagina,
  Recoleccion,
  Usuario,
  Venta,
} from './types'

// El contrato REST limita `size` a 20; el gateway lo reenvia tal cual, asi que nunca
// se piden mas de 20 elementos por pagina.
export const TAM_PAGINA = 20

const CAMPOS_BALANCE = 'costoTotal ingresoTotal balance actualizadoEn'
const CAMPOS_FINCA = 'id nombre municipio departamento areaTotalHectareas fechaCreacion'
const CAMPOS_LOTE =
  'id fincaId nombre variedad fechaSiembra areaHectareas densidadSiembra alturaMsnm phSuelo tipoSuelo temperaturaPromedio'
const CAMPOS_LABOR =
  'id loteId tipo descripcion fechaEjecucion jornales costoManoObra costoInsumos costoTotal fechaRegistro'
const CAMPOS_FERTILIZACION =
  'id loteId estado fecha composicion dosisKgPorArbol cantidadTotalKg costoInsumos costoManoObra costoTotal observaciones fechaRegistro'
const CAMPOS_RECOLECCION =
  'id loteId fecha pesoCascaraKg valorPorKgArroba recolector estadoGrano costoManoObra fechaRegistro'
const CAMPOS_VENTA = 'id loteId fecha cantidad precioPorArroba comprador montoTotal fechaRegistro'

// ------------------------------------------------------------------ Consultas

export type LoteConBalance = Lote & { balance: Balance | null }
export type FincaResumen = Finca & { lotes: LoteConBalance[]; balance: Balance | null }

/**
 * Una sola peticion al BFF arma todo el tablero: fincas (ms-agronomico), sus lotes
 * y el balance de cada uno (ms-financiero, resuelto en batch por el gateway).
 */
export function useResumen() {
  return useQuery({
    queryKey: ['resumen'],
    queryFn: () =>
      gql<{ fincas: Pagina<FincaResumen> }>(`
        query Resumen {
          fincas(page: 0, size: ${TAM_PAGINA}) {
            totalElements
            content {
              ${CAMPOS_FINCA}
              balance { ${CAMPOS_BALANCE} }
              lotes(page: 0, size: ${TAM_PAGINA}) { ${CAMPOS_LOTE} balance { ${CAMPOS_BALANCE} } }
            }
          }
        }
      `).then((d) => d.fincas),
  })
}

export function useFinca(id: string | undefined) {
  return useQuery({
    queryKey: ['finca', id],
    enabled: !!id,
    queryFn: () =>
      gql<{ finca: FincaResumen }>(
        `query Finca($id: ID!) {
          finca(id: $id) {
            ${CAMPOS_FINCA}
            balance { ${CAMPOS_BALANCE} }
            lotes(page: 0, size: ${TAM_PAGINA}) { ${CAMPOS_LOTE} balance { ${CAMPOS_BALANCE} } }
          }
        }`,
        { id },
      ).then((d) => d.finca),
  })
}

export function useLote(id: string | undefined) {
  return useQuery({
    queryKey: ['lote', id],
    enabled: !!id,
    queryFn: () =>
      gql<{ lote: LoteConBalance }>(
        `query Lote($id: ID!) { lote(id: $id) { ${CAMPOS_LOTE} balance { ${CAMPOS_BALANCE} } } }`,
        { id },
      ).then((d) => d.lote),
  })
}

/**
 * Cada seccion del lote se pide por separado: si un microservicio esta caido solo
 * falla su pestaña y el resto de la pantalla sigue funcionando.
 */
function useSeccionLote<T>(loteId: string | undefined, campo: string, campos: string) {
  return useQuery({
    queryKey: ['lote', loteId, campo],
    enabled: !!loteId,
    queryFn: () =>
      gql<{ lote: Record<string, T[]> }>(
        `query Seccion($id: ID!) { lote(id: $id) { id ${campo}(page: 0, size: ${TAM_PAGINA}) { ${campos} } } }`,
        { id: loteId },
      ).then((d) => d.lote[campo]),
  })
}

export const useLabores = (loteId?: string) => useSeccionLote<Labor>(loteId, 'labores', CAMPOS_LABOR)
export const useFertilizaciones = (loteId?: string) =>
  useSeccionLote<Fertilizacion>(loteId, 'fertilizaciones', CAMPOS_FERTILIZACION)
export const useRecolecciones = (loteId?: string) =>
  useSeccionLote<Recoleccion>(loteId, 'recolecciones', CAMPOS_RECOLECCION)
export const useVentasLote = (loteId?: string) => useSeccionLote<Venta>(loteId, 'ventas', CAMPOS_VENTA)

export function useVentas(pagina: number, loteId?: string) {
  return useQuery({
    queryKey: ['ventas', pagina, loteId ?? null],
    placeholderData: (previo) => previo,
    queryFn: () =>
      gql<{ ventas: Pagina<Venta> }>(
        `query Ventas($page: Int, $loteId: ID) {
          ventas(page: $page, size: 10, loteId: $loteId) {
            page size totalElements totalPages
            content { ${CAMPOS_VENTA} }
          }
        }`,
        { page: pagina, loteId: loteId ?? null },
      ).then((d) => d.ventas),
  })
}

export function usePerfil() {
  return useQuery({
    queryKey: ['me'],
    queryFn: () =>
      gql<{ me: Usuario }>('query { me { id nombre email rol estado fechaCreacion } }').then((d) => d.me),
  })
}

// ------------------------------------------------------------------ Mutaciones (GraphQL)

/** Quita cadenas vacias y NaN para que los opcionales viajen como null. */
export function limpiar<T extends Record<string, unknown>>(datos: T): T {
  const resultado: Record<string, unknown> = {}
  for (const [clave, valor] of Object.entries(datos)) {
    if (valor === '' || (typeof valor === 'number' && Number.isNaN(valor))) continue
    resultado[clave] = valor
  }
  return resultado as T
}

export const mutaciones = {
  crearFinca: (input: Record<string, unknown>) =>
    gql<{ crearFinca: Finca }>(
      `mutation($input: FincaInput!) { crearFinca(input: $input) { ${CAMPOS_FINCA} } }`,
      { input: limpiar(input) },
    ).then((d) => d.crearFinca),

  crearLote: (fincaId: string, input: Record<string, unknown>) =>
    gql<{ crearLote: Lote }>(
      `mutation($fincaId: ID!, $input: LoteInput!) { crearLote(fincaId: $fincaId, input: $input) { ${CAMPOS_LOTE} } }`,
      { fincaId, input: limpiar(input) },
    ).then((d) => d.crearLote),

  registrarLabor: (loteId: string, input: Record<string, unknown>, idempotencyKey: string) =>
    gql<{ registrarLabor: Labor }>(
      `mutation($loteId: ID!, $input: LaborInput!, $key: String) {
        registrarLabor(loteId: $loteId, input: $input, idempotencyKey: $key) { ${CAMPOS_LABOR} }
      }`,
      { loteId, input: limpiar(input), key: idempotencyKey },
    ).then((d) => d.registrarLabor),

  anularLabor: (laborId: string) =>
    gql<{ anularLabor: boolean }>('mutation($id: ID!) { anularLabor(laborId: $id) }', { id: laborId }),

  registrarFertilizacion: (loteId: string, input: Record<string, unknown>, idempotencyKey: string) =>
    gql<{ registrarFertilizacion: Fertilizacion }>(
      `mutation($loteId: ID!, $input: FertilizacionInput!, $key: String) {
        registrarFertilizacion(loteId: $loteId, input: $input, idempotencyKey: $key) { ${CAMPOS_FERTILIZACION} }
      }`,
      { loteId, input: limpiar(input), key: idempotencyKey },
    ).then((d) => d.registrarFertilizacion),

  ejecutarFertilizacion: (fertilizacionId: string, input: Record<string, unknown>) =>
    gql<{ ejecutarFertilizacion: Fertilizacion }>(
      `mutation($id: ID!, $input: EjecucionFertilizacionInput!) {
        ejecutarFertilizacion(fertilizacionId: $id, input: $input) { ${CAMPOS_FERTILIZACION} }
      }`,
      { id: fertilizacionId, input: limpiar(input) },
    ).then((d) => d.ejecutarFertilizacion),

  registrarRecoleccion: (loteId: string, input: Record<string, unknown>, idempotencyKey: string) =>
    gql<{ registrarRecoleccion: Recoleccion }>(
      `mutation($loteId: ID!, $input: RecoleccionInput!, $key: String) {
        registrarRecoleccion(loteId: $loteId, input: $input, idempotencyKey: $key) { ${CAMPOS_RECOLECCION} }
      }`,
      { loteId, input: limpiar(input), key: idempotencyKey },
    ).then((d) => d.registrarRecoleccion),

  actualizarEstadoGrano: (recoleccionId: string, estadoGrano: EstadoGrano) =>
    gql<{ actualizarEstadoGrano: Recoleccion }>(
      `mutation($id: ID!, $estado: EstadoGrano!) {
        actualizarEstadoGrano(recoleccionId: $id, estadoGrano: $estado) { ${CAMPOS_RECOLECCION} }
      }`,
      { id: recoleccionId, estado: estadoGrano },
    ).then((d) => d.actualizarEstadoGrano),

  registrarVenta: (input: Record<string, unknown>, idempotencyKey: string) =>
    gql<{ registrarVenta: Venta }>(
      `mutation($input: VentaInput!, $key: String) {
        registrarVenta(input: $input, idempotencyKey: $key) { ${CAMPOS_VENTA} }
      }`,
      { input: limpiar(input), key: idempotencyKey },
    ).then((d) => d.registrarVenta),

  anularVenta: (ventaId: string) =>
    gql<{ anularVenta: boolean }>('mutation($id: ID!) { anularVenta(ventaId: $id) }', { id: ventaId }),

  // --- Operaciones que el gateway aun no expone: van directo al microservicio (REST).
  actualizarFinca: (fincaId: string, input: Record<string, unknown>) =>
    rest<Finca>('agronomico', `/api/v1/fincas/${fincaId}`, { metodo: 'PUT', cuerpo: limpiar(input) }),

  actualizarLote: (loteId: string, input: Record<string, unknown>) =>
    rest<Lote>('agronomico', `/api/v1/lotes/${loteId}`, { metodo: 'PUT', cuerpo: limpiar(input) }),

  anularFertilizacion: (fertilizacionId: string) =>
    rest<void>('agronomico', `/api/v1/fertilizaciones/${fertilizacionId}`, { metodo: 'DELETE' }),

  anularRecoleccion: (recoleccionId: string) =>
    rest<void>('cosecha', `/api/v1/recolecciones/${recoleccionId}`, { metodo: 'DELETE' }),

  actualizarPerfil: (nombre: string) =>
    rest<Usuario>('auth', '/api/v1/usuarios/me', { metodo: 'PUT', cuerpo: { nombre } }),

  cambiarPassword: (passwordActual: string, passwordNueva: string) =>
    gql<{ cambiarPassword: boolean }>(
      /* GraphQL */ `
        mutation CambiarPassword($input: CambiarPasswordInput!) {
          cambiarPassword(input: $input)
        }
      `,
      { input: { passwordActual, passwordNueva } },
    ),
}

/**
 * Refresca las vistas tras una escritura. El balance lo consolida ms-financiero al
 * consumir el evento de Kafka, asi que llega unos instantes despues: se vuelve a
 * consultar un par de veces para reflejar esa consistencia eventual.
 */
export function refrescar(cliente: QueryClient) {
  const invalidar = () => {
    void cliente.invalidateQueries({ queryKey: ['resumen'] })
    void cliente.invalidateQueries({ queryKey: ['finca'] })
    void cliente.invalidateQueries({ queryKey: ['lote'] })
    void cliente.invalidateQueries({ queryKey: ['ventas'] })
  }
  invalidar()
  window.setTimeout(invalidar, 1500)
  window.setTimeout(invalidar, 4000)
}
