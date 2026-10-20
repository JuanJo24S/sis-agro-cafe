import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChartColumn, Coins, Percent, Receipt, Scale, Zap } from 'lucide-react'
import { useResumen } from '../api/agrocafe'
import { GraficaBalance, GraficaIngresosCostos } from '../components/graficas'
import { Cargando, Encabezado, ErrorCarga, Indicador, Saldo, Vacio } from '../components/ui'
import { num, pesos } from '../lib/formato'

export function Finanzas() {
  const resumen = useResumen()
  const navegar = useNavigate()
  const [fincaId, setFincaId] = useState('')
  const fincas = resumen.data?.content ?? []

  const filas = useMemo(
    () =>
      fincas
        .filter((f) => !fincaId || f.id === fincaId)
        .flatMap((f) =>
          f.lotes.map((l) => {
            const ingresos = l.balance?.ingresoTotal ?? 0
            const costos = l.balance?.costoTotal ?? 0
            return {
              id: l.id,
              lote: l.nombre,
              finca: f.nombre,
              area: l.areaHectareas,
              ingresos,
              costos,
              balance: ingresos - costos,
              porHectarea: l.areaHectareas > 0 ? (ingresos - costos) / l.areaHectareas : null,
              margen: ingresos > 0 ? (ingresos - costos) / ingresos : null,
            }
          }),
        )
        .sort((a, b) => b.balance - a.balance),
    [fincas, fincaId],
  )

  const ingresos = filas.reduce((s, f) => s + f.ingresos, 0)
  const costos = filas.reduce((s, f) => s + f.costos, 0)
  const balance = ingresos - costos

  return (
    <>
      <Encabezado
        titulo="Finanzas"
        descripcion="Rentabilidad por lote consolidada por ms-financiero a partir de los eventos de costos e ingresos."
        acciones={
          fincas.length > 1 && (
            <select className="selector" value={fincaId} onChange={(e) => setFincaId(e.target.value)} aria-label="Filtrar por finca">
              <option value="">Todas las fincas</option>
              {fincas.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nombre}
                </option>
              ))}
            </select>
          )
        }
      />

      {resumen.isLoading ? (
        <div className="tarjeta">
          <Cargando filas={5} />
        </div>
      ) : resumen.isError ? (
        <div className="tarjeta">
          <ErrorCarga error={resumen.error} reintentar={() => resumen.refetch()} />
        </div>
      ) : filas.length === 0 ? (
        <div className="tarjeta">
          <Vacio icono={ChartColumn} titulo="Aún no hay lotes" texto="Crea fincas y lotes para ver su rentabilidad." />
        </div>
      ) : (
        <>
          <section className="indicadores">
            <Indicador icono={Coins} etiqueta="Ingresos" valor={pesos(ingresos)} tono="verde" />
            <Indicador icono={Receipt} etiqueta="Costos" valor={pesos(costos)} tono="caramelo" />
            <Indicador icono={Scale} etiqueta="Balance" valor={<Saldo valor={balance} />} tono="neutro" />
            <Indicador icono={Percent} etiqueta="Margen" valor={ingresos > 0 ? `${Math.round((balance / ingresos) * 100)} %` : '—'} detalle="Balance sobre ingresos" />
          </section>

          <section className="rejilla-panel">
            <div className="tarjeta">
              <GraficaIngresosCostos
                titulo="Ingresos vs costos por lote"
                datos={filas.map((f) => ({ id: f.id, etiqueta: f.lote, ingresos: f.ingresos, costos: f.costos }))}
              />
            </div>
            <div className="tarjeta">
              <div className="tarjeta__cabecera">
                <h2>Ganancia o pérdida</h2>
              </div>
              <GraficaBalance
                alSeleccionar={(id) => navegar(`/lotes/${id}`)}
                datos={filas.map((f) => ({ id: f.id, etiqueta: f.lote, detalle: f.finca, valor: f.balance }))}
              />
            </div>
          </section>

          <div className="tarjeta">
            <div className="tarjeta__cabecera">
              <h2>Rentabilidad por lote</h2>
            </div>
            <div className="tabla-envoltura">
              <table className="tabla tabla--clicable">
                <thead>
                  <tr>
                    <th>Lote</th>
                    <th className="num">Área</th>
                    <th className="num">Ingresos</th>
                    <th className="num">Costos</th>
                    <th className="num">Balance</th>
                    <th className="num">Balance / ha</th>
                    <th className="num">Margen</th>
                  </tr>
                </thead>
                <tbody>
                  {filas.map((f) => (
                    <tr key={f.id} onClick={() => navegar(`/lotes/${f.id}`)}>
                      <td>
                        <span className="enlace-tabla">
                          {f.lote}
                          <small>{f.finca}</small>
                        </span>
                      </td>
                      <td className="num">{num(f.area)} ha</td>
                      <td className="num">{pesos(f.ingresos)}</td>
                      <td className="num">{pesos(f.costos)}</td>
                      <td className="num">
                        <Saldo valor={f.balance} />
                      </td>
                      <td className="num">{f.porHectarea != null ? pesos(f.porHectarea) : '—'}</td>
                      <td className="num">{f.margen != null ? `${Math.round(f.margen * 100)} %` : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <p className="fuente-datos">
            <Zap size={14} aria-hidden /> Consistencia eventual: los costos e ingresos viajan por Kafka y el balance puede tardar unos
            segundos en reflejar un registro nuevo.
          </p>
        </>
      )}
    </>
  )
}
