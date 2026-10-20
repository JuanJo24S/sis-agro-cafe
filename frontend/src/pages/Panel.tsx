import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowRight, Coins, HandCoins, Loader2, Plus, Receipt, Scale, Sprout, Trees, WandSparkles } from 'lucide-react'
import { refrescar, useResumen, useVentas } from '../api/agrocafe'
import { useAvisos } from '../components/avisos'
import { GraficaBalance, GraficaIngresosCostos } from '../components/graficas'
import { Cargando, Encabezado, ErrorCarga, Indicador, Saldo, Vacio } from '../components/ui'
import { FormularioFinca, FormularioVenta } from '../forms/formularios'
import { useAuth } from '../lib/auth'
import { cargarDemo } from '../lib/demo'
import { fecha, num, pesos } from '../lib/formato'
import { mensajeDe } from '../lib/useAccion'

function saludo() {
  const hora = new Date().getHours()
  return hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches'
}

export function Panel() {
  const { usuario } = useAuth()
  const resumen = useResumen()
  const ventas = useVentas(0)
  const navegar = useNavigate()
  const [modal, setModal] = useState<'finca' | 'venta' | null>(null)

  const fincas = resumen.data?.content ?? []
  const totales = useMemo(() => {
    const lotes = fincas.flatMap((f) => f.lotes)
    const ingresos = fincas.reduce((s, f) => s + (f.balance?.ingresoTotal ?? 0), 0)
    const costos = fincas.reduce((s, f) => s + (f.balance?.costoTotal ?? 0), 0)
    return {
      lotes,
      hectareas: lotes.reduce((s, l) => s + (l.areaHectareas ?? 0), 0),
      variedades: new Set(lotes.map((l) => l.variedad)).size,
      ingresos,
      costos,
      balance: ingresos - costos,
    }
  }, [fincas])

  const nombreLote = useMemo(() => {
    const mapa = new Map<string, string>()
    fincas.forEach((f) => f.lotes.forEach((l) => mapa.set(l.id, `${l.nombre} · ${f.nombre}`)))
    return mapa
  }, [fincas])

  const primerNombre = usuario?.nombre.split(' ')[0] ?? ''
  const hoy = new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' })

  return (
    <>
      <Encabezado
        titulo={`${saludo()}, ${primerNombre}`}
        descripcion={<span className="capitalizar">{hoy}</span>}
        acciones={
          fincas.length > 0 && (
            <>
              <button className="btn btn--secundario" onClick={() => setModal('finca')}>
                <Plus size={16} /> Nueva finca
              </button>
              <button className="btn btn--primario" onClick={() => setModal('venta')}>
                <HandCoins size={16} /> Registrar venta
              </button>
            </>
          )
        }
      />

      {resumen.isLoading ? (
        <div className="tarjeta">
          <Cargando filas={4} />
        </div>
      ) : resumen.isError ? (
        <div className="tarjeta">
          <ErrorCarga error={resumen.error} reintentar={() => resumen.refetch()} />
        </div>
      ) : fincas.length === 0 ? (
        <Bienvenida alCrearFinca={() => setModal('finca')} />
      ) : (
        <>
          <section className="indicadores">
            <Indicador icono={Trees} etiqueta="Fincas" valor={fincas.length} detalle={`${num(totales.hectareas)} ha en lotes`} />
            <Indicador icono={Sprout} etiqueta="Lotes" valor={totales.lotes.length} detalle={`${totales.variedades} ${totales.variedades === 1 ? 'variedad' : 'variedades'}`} tono="verde" />
            <Indicador icono={Coins} etiqueta="Ingresos" valor={pesos(totales.ingresos)} detalle="Ventas consolidadas" tono="verde" />
            <Indicador icono={Receipt} etiqueta="Costos" valor={pesos(totales.costos)} detalle="Labores, abonos y cosecha" tono="caramelo" />
          </section>

          <section className="rejilla-panel">
            <div className="tarjeta tarjeta--destacada">
              <div className="balance-hero">
                <span className="balance-hero__etiqueta">
                  <Scale size={16} aria-hidden /> Balance general
                </span>
                <Saldo valor={totales.balance} grande />
                <p>
                  {totales.ingresos > 0
                    ? `Margen del ${Math.round((totales.balance / totales.ingresos) * 100)} % sobre las ventas`
                    : 'Aún no hay ventas registradas'}
                </p>
              </div>
              <GraficaIngresosCostos
                titulo="Ingresos vs costos por finca"
                datos={fincas.map((f) => ({
                  id: f.id,
                  etiqueta: f.nombre,
                  ingresos: f.balance?.ingresoTotal ?? 0,
                  costos: f.balance?.costoTotal ?? 0,
                }))}
              />
            </div>

            <div className="tarjeta">
              <div className="tarjeta__cabecera">
                <h2>Balance por lote</h2>
                <Link to="/finanzas" className="enlace">
                  Ver finanzas <ArrowRight size={14} />
                </Link>
              </div>
              <GraficaBalance
                alSeleccionar={(id) => navegar(`/lotes/${id}`)}
                datos={fincas
                  .flatMap((f) => f.lotes.map((l) => ({ l, f })))
                  .map(({ l, f }) => ({ id: l.id, etiqueta: l.nombre, detalle: f.nombre, valor: l.balance?.balance ?? 0 }))
                  .sort((a, b) => b.valor - a.valor)
                  .slice(0, 8)}
              />
            </div>
          </section>

          <section className="rejilla-panel rejilla-panel--inversa">
            <div className="tarjeta">
              <div className="tarjeta__cabecera">
                <h2>Ventas recientes</h2>
                <Link to="/ventas" className="enlace">
                  Ver todas <ArrowRight size={14} />
                </Link>
              </div>
              {ventas.isLoading ? (
                <Cargando />
              ) : ventas.isError ? (
                <ErrorCarga error={ventas.error} reintentar={() => ventas.refetch()} />
              ) : ventas.data?.content.length ? (
                <ul className="lista">
                  {ventas.data.content.slice(0, 5).map((v) => (
                    <li key={v.id} className="lista__item">
                      <span className="lista__icono lista__icono--verde">
                        <HandCoins size={17} aria-hidden />
                      </span>
                      <div className="lista__texto">
                        <strong>{v.comprador}</strong>
                        <span>
                          {num(v.cantidad)} @ · {nombreLote.get(v.loteId) ?? 'Lote'} · {fecha(v.fecha)}
                        </span>
                      </div>
                      <strong className="lista__monto">{pesos(v.montoTotal)}</strong>
                    </li>
                  ))}
                </ul>
              ) : (
                <Vacio icono={HandCoins} titulo="Sin ventas todavía" texto="Registra la primera venta de café." />
              )}
            </div>

            <div className="tarjeta">
              <div className="tarjeta__cabecera">
                <h2>Mis fincas</h2>
                <Link to="/fincas" className="enlace">
                  Gestionar <ArrowRight size={14} />
                </Link>
              </div>
              <ul className="lista">
                {fincas.map((f) => (
                  <li key={f.id}>
                    <Link to={`/fincas/${f.id}`} className="lista__item lista__item--enlace">
                      <span className="lista__icono">
                        <Trees size={17} aria-hidden />
                      </span>
                      <div className="lista__texto">
                        <strong>{f.nombre}</strong>
                        <span>
                          {f.municipio}
                          {f.departamento ? `, ${f.departamento}` : ''} · {f.lotes.length} {f.lotes.length === 1 ? 'lote' : 'lotes'}
                        </span>
                      </div>
                      <Saldo valor={f.balance?.balance ?? 0} />
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </section>
          <p className="fuente-datos">
            Tablero armado con una sola consulta al Gateway GraphQL: fincas y lotes (ms-agronómico) + balances (ms-financiero).
          </p>
        </>
      )}

      <FormularioFinca abierto={modal === 'finca'} alCerrar={() => setModal(null)} />
      <FormularioVenta abierto={modal === 'venta'} alCerrar={() => setModal(null)} />
    </>
  )
}

function Bienvenida({ alCrearFinca }: { alCrearFinca: () => void }) {
  const cliente = useQueryClient()
  const avisos = useAvisos()
  const [progreso, setProgreso] = useState<{ texto: string; avance: number } | null>(null)

  const demo = async () => {
    setProgreso({ texto: 'Preparando…', avance: 0 })
    try {
      await cargarDemo((texto, avance) => setProgreso({ texto, avance }))
      avisos.exito('Datos de demostración cargados', 'Los balances terminan de consolidarse en unos segundos.')
    } catch (e) {
      avisos.error('No se pudieron cargar los datos de demostración', mensajeDe(e))
    } finally {
      setProgreso(null)
      refrescar(cliente)
    }
  }

  return (
    <section className="bienvenida">
      <img src="/logo-agrocafe-icon.png" alt="" width={84} height={84} />
      <h2>Empecemos por tu primera finca</h2>
      <p>
        Registra una finca y sus lotes para llevar la bitácora de labores, la fertilización, la cosecha y las ventas. El balance de
        cada lote se calcula solo.
      </p>
      {progreso ? (
        <div className="progreso" role="status">
          <div className="progreso__barra">
            <span style={{ width: `${Math.round(progreso.avance * 100)}%` }} />
          </div>
          <span>
            <Loader2 size={14} className="girar" aria-hidden /> {progreso.texto}
          </span>
        </div>
      ) : (
        <div className="bienvenida__acciones">
          <button className="btn btn--primario" onClick={alCrearFinca}>
            <Plus size={16} /> Crear mi primera finca
          </button>
          <button className="btn btn--secundario" onClick={demo}>
            <WandSparkles size={16} /> Cargar datos de demostración
          </button>
        </div>
      )}
    </section>
  )
}
