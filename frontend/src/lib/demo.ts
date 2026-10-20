import { mutaciones } from '../api/agrocafe'
import type { EstadoGrano } from '../api/types'
import { hoyISO } from './formato'
import { nuevaClave } from './sesion'

/**
 * Carga un escenario de demostracion completo usando las mismas mutaciones GraphQL
 * de la interfaz: 2 fincas, 3 lotes y su historia (labores, abonadas, cosecha y ventas).
 * Cada costo e ingreso viaja por Kafka hasta ms-financiero, que arma el balance.
 */
interface LoteDemo {
  datos: Record<string, unknown>
  labores: [string, string, number, number, number, number][] // tipo, descripcion, diasAtras, jornales, manoObra, insumos
  abonadas: [string, number, number, number, number][] // composicion, dias (<0 ejecutada, >0 programada), dosis, insumos, manoObra
  recolecciones: [number, number, number, string, EstadoGrano][] // diasAtras, kg, pagoKg, recolector, estadoFinal
  ventas: [number, number, number, string][] // diasAtras, arrobas, precio, comprador
}

const ESCENARIO: { finca: Record<string, unknown>; lotes: LoteDemo[] }[] = [
  {
    finca: { nombre: 'La Esperanza', municipio: 'Pitalito', departamento: 'Huila', areaTotalHectareas: 12.5 },
    lotes: [
      {
        datos: {
          nombre: 'El Mirador', variedad: 'Castillo', fechaSiembra: '2019-03-15', areaHectareas: 3.2,
          densidadSiembra: 5000, alturaMsnm: 1650, phSuelo: 5.4, tipoSuelo: 'Franco arcilloso', temperaturaPromedio: 19.5,
        },
        labores: [
          ['LIMPIA', 'Plateo y limpia general', 75, 6, 330000, 0],
          ['FUMIGACION', 'Control preventivo de broca', 48, 2, 120000, 185000],
          ['PODA', 'Poda de formación', 20, 4, 220000, 0],
        ],
        abonadas: [
          ['17-6-18-2', -60, 0.05, 780000, 160000],
          ['DAP 18-46-0', 25, 0.04, 650000, 140000],
        ],
        recolecciones: [
          [40, 420, 900, 'Cuadrilla Rodríguez', 'ALMACENADO'],
          [26, 380, 900, 'Cuadrilla Rodríguez', 'SECO_TRILLA'],
          [9, 310, 950, 'Don Arley Muñoz', 'MOJADO'],
        ],
        ventas: [
          [18, 32, 182000, 'Cooperativa de Caficultores del Huila'],
          [5, 21, 186500, 'Federación Nacional de Cafeteros'],
        ],
      },
      {
        datos: {
          nombre: 'La Cañada', variedad: 'Caturra', fechaSiembra: '2016-08-10', areaHectareas: 2.4,
          densidadSiembra: 4500, alturaMsnm: 1720, phSuelo: 5.1, tipoSuelo: 'Franco', temperaturaPromedio: 18.8,
        },
        labores: [
          ['DESHIERBE', 'Deshierbe manual entre surcos', 66, 5, 275000, 0],
          ['FUMIGACION', 'Manejo de roya', 30, 2, 110000, 240000],
        ],
        abonadas: [['Urea 46%', -45, 0.03, 520000, 120000]],
        recolecciones: [
          [33, 290, 900, 'Cuadrilla Perdomo', 'ALMACENADO'],
          [12, 260, 950, 'Cuadrilla Perdomo', 'SECO_AGUA'],
        ],
        ventas: [[14, 24, 180000, 'Trilladora local']],
      },
    ],
  },
  {
    finca: { nombre: 'Villa Rosa', municipio: 'Garzón', departamento: 'Huila', areaTotalHectareas: 6 },
    lotes: [
      {
        datos: {
          nombre: 'Alto Bonito', variedad: 'Cenicafé 1', fechaSiembra: '2023-05-02', areaHectareas: 1.8,
          densidadSiembra: 6000, alturaMsnm: 1580, phSuelo: 5.8, tipoSuelo: 'Arenoso', temperaturaPromedio: 20.4,
        },
        labores: [
          ['LIMPIA', 'Limpia de levante', 58, 4, 220000, 0],
          ['OTRA', 'Resiembra de colinos', 34, 3, 165000, 210000],
        ],
        abonadas: [
          ['Abono orgánico (compost)', -50, 0.5, 450000, 110000],
          ['17-6-18-2', 40, 0.04, 380000, 90000],
        ],
        recolecciones: [[15, 140, 950, 'Familia Cuéllar', 'SECO_AGUA']],
        ventas: [[6, 6, 184000, 'Trilladora local']],
      },
    ],
  },
]

export async function cargarDemo(progreso: (texto: string, avance: number) => void) {
  const total = ESCENARIO.reduce(
    (n, f) => n + 1 + f.lotes.reduce((m, l) => m + 1 + l.labores.length + l.abonadas.length + l.recolecciones.length * 2 + l.ventas.length, 0),
    0,
  )
  let hechos = 0
  const paso = (texto: string) => progreso(texto, ++hechos / total)

  for (const { finca, lotes } of ESCENARIO) {
    const creada = await mutaciones.crearFinca(finca)
    paso(`Finca ${creada.nombre}`)
    for (const lote of lotes) {
      const nuevo = await mutaciones.crearLote(creada.id, lote.datos)
      paso(`Lote ${nuevo.nombre}`)

      for (const [tipo, descripcion, dias, jornales, costoManoObra, costoInsumos] of lote.labores) {
        await mutaciones.registrarLabor(
          nuevo.id,
          { tipo, descripcion, fechaEjecucion: hoyISO(-dias), jornales, costoManoObra, costoInsumos },
          nuevaClave(),
        )
        paso(`Labor en ${nuevo.nombre}`)
      }
      for (const [composicion, dias, dosis, costoInsumos, costoManoObra] of lote.abonadas) {
        const programada = dias > 0
        await mutaciones.registrarFertilizacion(
          nuevo.id,
          {
            estado: programada ? 'PROGRAMADA' : 'EJECUTADA',
            fecha: hoyISO(dias),
            composicion,
            dosisKgPorArbol: dosis,
            costoInsumos,
            costoManoObra,
          },
          nuevaClave(),
        )
        paso(`Abonada en ${nuevo.nombre}`)
      }
      for (const [dias, kg, pago, recolector, estadoFinal] of lote.recolecciones) {
        const r = await mutaciones.registrarRecoleccion(
          nuevo.id,
          { fecha: hoyISO(-dias), pesoCascaraKg: kg, valorPorKgArroba: pago, recolector },
          nuevaClave(),
        )
        paso(`Recolección en ${nuevo.nombre}`)
        if (estadoFinal !== 'CEREZA') await mutaciones.actualizarEstadoGrano(r.id, estadoFinal)
        paso(`Trazabilidad del grano`)
      }
      for (const [dias, cantidad, precioPorArroba, comprador] of lote.ventas) {
        await mutaciones.registrarVenta(
          { loteId: nuevo.id, fecha: hoyISO(-dias), cantidad, precioPorArroba, comprador },
          nuevaClave(),
        )
        paso(`Venta de ${nuevo.nombre}`)
      }
    }
  }
}
