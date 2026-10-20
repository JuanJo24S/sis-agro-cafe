import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Coins, HandCoins, Plus, Receipt, Trash2 } from 'lucide-react'
import { mutaciones, useResumen, useVentas } from '../api/agrocafe'
import { Cargando, Confirmar, Encabezado, ErrorCarga, Indicador, Vacio } from '../components/ui'
import { FormularioVenta } from '../forms/formularios'
import { fecha, num, pesos } from '../lib/formato'
import { useAccion } from '../lib/useAccion'
import type { Venta } from '../api/types'

export function Ventas() {
  const [pagina, setPagina] = useState(0)
  const [loteId, setLoteId] = useState('')
  const [nueva, setNueva] = useState(false)
  const [anulando, setAnulando] = useState<Venta | null>(null)
  const resumen = useResumen()
  const ventas = useVentas(pagina, loteId || undefined)
  const anular = useAccion(mutaciones.anularVenta, {
    exito: 'Venta anulada',
    detalle: 'Se publicó el ingreso compensatorio.',
    avisarError: true,
    alTerminar: () => setAnulando(null),
  })

  const fincas = resumen.data?.content ?? []
  const lotes = useMemo(() => {
    const mapa = new Map<string, { lote: string; finca: string; fincaId: string }>()
    fincas.forEach((f) => f.lotes.forEach((l) => mapa.set(l.id, { lote: l.nombre, finca: f.nombre, fincaId: f.id })))
    return mapa
  }, [fincas])
  const ingresos = fincas.reduce((s, f) => s + (f.balance?.ingresoTotal ?? 0), 0)
  const datos = ventas.data
  const montoPagina = datos?.content.reduce((s, v) => s + v.montoTotal, 0) ?? 0
  const arrobasPagina = datos?.content.reduce((s, v) => s + v.cantidad, 0) ?? 0

  return (
    <>
      <Encabezado
        titulo="Ventas"
        descripcion="Ventas de café por lote. Cada registro publica un ingreso que consolida ms-financiero."
        acciones={
          <button className="btn btn--primario" onClick={() => setNueva(true)}>
            <Plus size={16} /> Registrar venta
          </button>
        }
      />

      <section className="indicadores indicadores--3">
        <Indicador icono={Coins} etiqueta="Ingresos consolidados" valor={pesos(ingresos)} detalle="Todas las fincas" tono="verde" />
        <Indicador icono={Receipt} etiqueta="Ventas registradas" valor={datos ? num(datos.totalElements) : '—'} detalle={loteId ? 'En el lote filtrado' : 'En total'} />
        <Indicador
          icono={HandCoins}
          etiqueta="Precio promedio"
          valor={arrobasPagina > 0 ? `${pesos(montoPagina / arrobasPagina)} / @` : '—'}
          detalle="Ventas de esta página"
          tono="caramelo"
        />
      </section>

      <div className="tarjeta">
        <div className="seccion__cabecera">
          <h2>Historial</h2>
          <select
            className="selector"
            value={loteId}
            onChange={(e) => {
              setLoteId(e.target.value)
              setPagina(0)
            }}
            aria-label="Filtrar por lote"
          >
            <option value="">Todos los lotes</option>
            {fincas.map((f) => (
              <optgroup key={f.id} label={f.nombre}>
                {f.lotes.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.nombre}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
        </div>

        {ventas.isLoading ? (
          <Cargando filas={5} />
        ) : ventas.isError ? (
          <ErrorCarga error={ventas.error} reintentar={() => ventas.refetch()} />
        ) : !datos || datos.content.length === 0 ? (
          <Vacio
            icono={HandCoins}
            titulo="No hay ventas"
            texto={loteId ? 'Este lote aún no registra ventas.' : 'Registra tu primera venta de café.'}
          />
        ) : (
          <>
            <div className="tabla-envoltura">
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Comprador</th>
                    <th>Lote</th>
                    <th className="num">Cantidad</th>
                    <th className="num">Precio / @</th>
                    <th className="num">Total</th>
                    <th aria-label="Acciones" />
                  </tr>
                </thead>
                <tbody>
                  {datos.content.map((v) => {
                    const ref = lotes.get(v.loteId)
                    return (
                      <tr key={v.id}>
                        <td className="nowrap">{fecha(v.fecha)}</td>
                        <td>{v.comprador}</td>
                        <td>
                          {ref ? (
                            <Link to={`/lotes/${v.loteId}`} className="enlace-tabla">
                              {ref.lote}
                              <small>{ref.finca}</small>
                            </Link>
                          ) : (
                            <span className="texto-tenue">—</span>
                          )}
                        </td>
                        <td className="num">{num(v.cantidad)} @</td>
                        <td className="num">{pesos(v.precioPorArroba)}</td>
                        <td className="num">
                          <strong>{pesos(v.montoTotal)}</strong>
                        </td>
                        <td className="acciones-fila">
                          <button className="icono-btn icono-btn--peligro" onClick={() => setAnulando(v)} title="Anular venta" aria-label="Anular venta">
                            <Trash2 size={16} />
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            {datos.totalPages > 1 && (
              <nav className="paginacion" aria-label="Paginación">
                <button className="btn btn--fantasma btn--sm" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
                  <ChevronLeft size={16} /> Anterior
                </button>
                <span>
                  Página {datos.page + 1} de {datos.totalPages}
                </span>
                <button className="btn btn--fantasma btn--sm" disabled={pagina >= datos.totalPages - 1} onClick={() => setPagina((p) => p + 1)}>
                  Siguiente <ChevronRight size={16} />
                </button>
              </nav>
            )}
          </>
        )}
      </div>

      <FormularioVenta abierto={nueva} alCerrar={() => setNueva(false)} loteId={loteId || undefined} />
      <Confirmar
        abierto={!!anulando}
        titulo="Anular venta"
        mensaje={
          anulando &&
          `La venta a ${anulando.comprador} por ${pesos(anulando.montoTotal)} quedará anulada y su ingreso se descontará del balance.`
        }
        textoAccion="Anular venta"
        peligro
        ocupado={anular.ocupado}
        alConfirmar={() => anulando && anular.ejecutar(anulando.id)}
        alCerrar={() => setAnulando(null)}
      />
    </>
  )
}
