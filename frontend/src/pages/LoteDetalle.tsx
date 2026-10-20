import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  CalendarCheck,
  CalendarClock,
  Check,
  ChevronRight,
  FlaskConical,
  HandCoins,
  Leaf,
  Pencil,
  Plus,
  Scale,
  Trash2,
  Weight,
} from 'lucide-react'
import {
  mutaciones,
  useFertilizaciones,
  useFinca,
  useLabores,
  useLote,
  useRecolecciones,
  useVentasLote,
} from '../api/agrocafe'
import type { EstadoGrano, Fertilizacion, Recoleccion } from '../api/types'
import { Cargando, Confirmar, Dato, Encabezado, ErrorCarga, Insignia, Saldo, Vacio } from '../components/ui'
import {
  FormularioEjecucion,
  FormularioFertilizacion,
  FormularioLabor,
  FormularioLote,
  FormularioRecoleccion,
  FormularioVenta,
  ICONOS_LABOR,
} from '../forms/formularios'
import {
  ESTADOS_FERTILIZACION,
  ESTADOS_GRANO,
  ORDEN_GRANO,
  TIPOS_LABOR,
  edadCultivo,
  fecha,
  fechaHora,
  num,
  pesos,
} from '../lib/formato'
import { useAccion } from '../lib/useAccion'

type Pestana = 'labores' | 'fertilizacion' | 'cosecha' | 'ventas'
type Modal = 'editar' | 'labor' | 'fertilizacion' | 'recoleccion' | 'venta' | null

interface Pendiente {
  titulo: string
  mensaje: ReactNode
  textoAccion: string
  peligro?: boolean
  ejecutar: () => void
}

export function LoteDetalle() {
  const { loteId } = useParams()
  const lote = useLote(loteId)
  const finca = useFinca(lote.data?.fincaId)
  const labores = useLabores(loteId)
  const fertilizaciones = useFertilizaciones(loteId)
  const recolecciones = useRecolecciones(loteId)
  const ventas = useVentasLote(loteId)

  const [pestana, setPestana] = useState<Pestana>('labores')
  const [modal, setModal] = useState<Modal>(null)
  const [ejecutando, setEjecutando] = useState<Fertilizacion | null>(null)
  const [pendiente, setPendiente] = useState<Pendiente | null>(null)

  const cerrarConfirmacion = () => setPendiente(null)
  const opciones = { avisarError: true, alTerminar: cerrarConfirmacion }
  const anularLabor = useAccion(mutaciones.anularLabor, { ...opciones, exito: 'Labor anulada', detalle: 'Se publicó el costo compensatorio.' })
  const anularFert = useAccion(mutaciones.anularFertilizacion, { ...opciones, exito: 'Fertilización anulada' })
  const anularRecol = useAccion(mutaciones.anularRecoleccion, { ...opciones, exito: 'Recolección anulada' })
  const anularVenta = useAccion(mutaciones.anularVenta, { ...opciones, exito: 'Venta anulada', detalle: 'Se publicó el ingreso compensatorio.' })
  const avanzarGrano = useAccion(mutaciones.actualizarEstadoGrano, {
    ...opciones,
    exito: (r) => `Grano en estado "${ESTADOS_GRANO[r.estadoGrano]}"`,
  })
  const ocupado = anularLabor.ocupado || anularFert.ocupado || anularRecol.ocupado || anularVenta.ocupado || avanzarGrano.ocupado

  if (lote.isLoading) {
    return (
      <div className="tarjeta">
        <Cargando filas={5} />
      </div>
    )
  }
  if (!lote.data) {
    return (
      <div className="tarjeta">
        <ErrorCarga error={lote.error} reintentar={() => lote.refetch()} />
      </div>
    )
  }

  const l = lote.data
  const arboles = l.densidadSiembra ? Math.round(l.densidadSiembra * l.areaHectareas) : null
  const confirmarAnulacion = (que: string, ejecutar: () => void, detalle?: string) =>
    setPendiente({
      titulo: `Anular ${que}`,
      mensaje: detalle ?? `El registro quedará anulado y el balance del lote se ajustará automáticamente.`,
      textoAccion: 'Anular',
      peligro: true,
      ejecutar,
    })

  const pestanas: { id: Pestana; etiqueta: string; icono: typeof Leaf; total?: number }[] = [
    { id: 'labores', etiqueta: 'Labores', icono: Leaf, total: labores.data?.length },
    { id: 'fertilizacion', etiqueta: 'Fertilización', icono: FlaskConical, total: fertilizaciones.data?.length },
    { id: 'cosecha', etiqueta: 'Cosecha', icono: Weight, total: recolecciones.data?.length },
    { id: 'ventas', etiqueta: 'Ventas', icono: HandCoins, total: ventas.data?.length },
  ]

  return (
    <>
      <Encabezado
        migas={
          <>
            <Link to="/fincas">Fincas</Link>
            <ChevronRight size={14} aria-hidden />
            <Link to={`/fincas/${l.fincaId}`}>{finca.data?.nombre ?? 'Finca'}</Link>
            <ChevronRight size={14} aria-hidden />
            <span>{l.nombre}</span>
          </>
        }
        titulo={
          <span className="en-linea">
            {l.nombre} <Insignia tono="cafe">{l.variedad}</Insignia>
          </span>
        }
        descripcion={`${num(l.areaHectareas)} ha · ${edadCultivo(l.fechaSiembra)} desde la siembra`}
        acciones={
          <button className="btn btn--secundario" onClick={() => setModal('editar')}>
            <Pencil size={16} /> Editar lote
          </button>
        }
      />

      <section className="rejilla-lote">
        <div className="tarjeta">
          <div className="tarjeta__cabecera">
            <h2>Ficha técnica</h2>
          </div>
          <dl className="datos">
            <Dato etiqueta="Variedad" valor={l.variedad} />
            <Dato etiqueta="Fecha de siembra" valor={fecha(l.fechaSiembra)} />
            <Dato etiqueta="Área" valor={`${num(l.areaHectareas)} ha`} />
            <Dato etiqueta="Densidad" valor={l.densidadSiembra ? `${num(l.densidadSiembra)} árboles/ha` : '—'} />
            <Dato etiqueta="Árboles estimados" valor={arboles ? num(arboles) : '—'} />
            <Dato etiqueta="Altura" valor={l.alturaMsnm ? `${num(l.alturaMsnm)} m s.n.m.` : '—'} />
            <Dato etiqueta="pH del suelo" valor={num(l.phSuelo)} />
            <Dato etiqueta="Tipo de suelo" valor={l.tipoSuelo || '—'} />
            <Dato etiqueta="Temperatura" valor={l.temperaturaPromedio != null ? `${num(l.temperaturaPromedio)} °C` : '—'} />
          </dl>
        </div>

        <div className="tarjeta tarjeta--destacada">
          <div className="tarjeta__cabecera">
            <h2>
              <Scale size={18} aria-hidden /> Balance del lote
            </h2>
          </div>
          {l.balance ? (
            <div className="balance-lote">
              <Saldo valor={l.balance.balance} grande />
              <div className="balance-lote__filas">
                <div>
                  <span>
                    <i className="muestra muestra--ingresos" aria-hidden /> Ingresos
                  </span>
                  <strong>{pesos(l.balance.ingresoTotal)}</strong>
                </div>
                <div>
                  <span>
                    <i className="muestra muestra--costos" aria-hidden /> Costos
                  </span>
                  <strong>{pesos(l.balance.costoTotal)}</strong>
                </div>
              </div>
              <ProporcionBalance ingresos={l.balance.ingresoTotal} costos={l.balance.costoTotal} />
              <p className="texto-tenue">Actualizado {fechaHora(l.balance.actualizadoEn)} · consolidado por ms-financiero desde Kafka</p>
            </div>
          ) : (
            <Vacio icono={Scale} titulo="Sin movimientos aún" texto="Registra labores, cosechas o ventas y el balance aparecerá aquí." />
          )}
        </div>
      </section>

      <div className="pestanas" role="tablist">
        {pestanas.map(({ id, etiqueta, icono: Icono, total }) => (
          <button key={id} role="tab" aria-selected={pestana === id} className={`pestana ${pestana === id ? 'pestana--activa' : ''}`} onClick={() => setPestana(id)}>
            <Icono size={17} aria-hidden /> {etiqueta}
            {total != null && <span className="pestana__total">{total}</span>}
          </button>
        ))}
      </div>

      <div className="tarjeta tarjeta--pestana" role="tabpanel">
        {pestana === 'labores' && (
          <Seccion
            titulo="Bitácora de labores"
            descripcion="Labores culturales ejecutadas en el lote."
            accion={<BotonNuevo onClick={() => setModal('labor')}>Registrar labor</BotonNuevo>}
            consulta={labores}
            vacio={<Vacio icono={Leaf} titulo="Sin labores registradas" texto="Limpias, fumigaciones, podas… todo suma al costo del lote." />}
          >
            {(datos) => (
              <div className="tabla-envoltura">
                <table className="tabla">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Labor</th>
                      <th className="num">Jornales</th>
                      <th className="num">Mano de obra</th>
                      <th className="num">Insumos</th>
                      <th className="num">Total</th>
                      <th aria-label="Acciones" />
                    </tr>
                  </thead>
                  <tbody>
                    {datos.map((x) => {
                      const Icono = ICONOS_LABOR[x.tipo]
                      return (
                        <tr key={x.id}>
                          <td className="nowrap">{fecha(x.fechaEjecucion)}</td>
                          <td>
                            <span className="celda-icono">
                              <span className="lista__icono lista__icono--sm">
                                <Icono size={14} aria-hidden />
                              </span>
                              <span>
                                <strong>{TIPOS_LABOR[x.tipo]}</strong>
                                {x.descripcion && <small>{x.descripcion}</small>}
                              </span>
                            </span>
                          </td>
                          <td className="num">{num(x.jornales)}</td>
                          <td className="num">{pesos(x.costoManoObra)}</td>
                          <td className="num">{pesos(x.costoInsumos)}</td>
                          <td className="num">
                            <strong>{pesos(x.costoTotal)}</strong>
                          </td>
                          <td className="acciones-fila">
                            <BotonAnular onClick={() => confirmarAnulacion('labor', () => anularLabor.ejecutar(x.id))} />
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colSpan={5}>Total en labores</td>
                      <td className="num">{pesos(datos.reduce((s, x) => s + x.costoTotal, 0))}</td>
                      <td />
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}
          </Seccion>
        )}

        {pestana === 'fertilizacion' && (
          <Seccion
            titulo="Plan de fertilización"
            descripcion="Abonadas aplicadas y programadas. Las programadas no generan costo hasta ejecutarse."
            accion={<BotonNuevo onClick={() => setModal('fertilizacion')}>Nueva abonada</BotonNuevo>}
            consulta={fertilizaciones}
            vacio={<Vacio icono={FlaskConical} titulo="Sin abonadas" texto="Registra una abonada aplicada o programa la próxima." />}
          >
            {(datos) => (
              <ul className="lista lista--tarjetas">
                {[...datos]
                  .sort((a, b) => Number(b.estado === 'PROGRAMADA') - Number(a.estado === 'PROGRAMADA'))
                  .map((x) => (
                    <li key={x.id} className={`abonada abonada--${x.estado.toLowerCase()}`}>
                      <span className="lista__icono">
                        {x.estado === 'PROGRAMADA' ? <CalendarClock size={18} aria-hidden /> : <CalendarCheck size={18} aria-hidden />}
                      </span>
                      <div className="lista__texto">
                        <strong>
                          {x.composicion}{' '}
                          <Insignia tono={x.estado === 'EJECUTADA' ? 'exito' : x.estado === 'PROGRAMADA' ? 'aviso' : 'neutro'}>
                            {ESTADOS_FERTILIZACION[x.estado]}
                          </Insignia>
                        </strong>
                        <span>
                          {fecha(x.fecha)}
                          {x.dosisKgPorArbol != null && ` · ${num(x.dosisKgPorArbol)} kg/árbol`}
                          {x.cantidadTotalKg != null && ` · ${num(x.cantidadTotalKg)} kg en total`}
                          {x.observaciones && ` · ${x.observaciones}`}
                        </span>
                      </div>
                      <div className="abonada__lado">
                        <strong className={x.estado === 'EJECUTADA' ? '' : 'texto-tenue'}>{pesos(x.costoTotal)}</strong>
                        <div className="acciones-fila">
                          {x.estado === 'PROGRAMADA' && (
                            <button className="btn btn--secundario btn--sm" onClick={() => setEjecutando(x)}>
                              <Check size={14} /> Ejecutar
                            </button>
                          )}
                          {x.estado !== 'CANCELADA' && (
                            <BotonAnular
                              onClick={() =>
                                confirmarAnulacion(
                                  'fertilización',
                                  () => anularFert.ejecutar(x.id),
                                  x.estado === 'EJECUTADA'
                                    ? 'La abonada ya se ejecutó: al anularla se publica el costo compensatorio en el balance.'
                                    : 'La abonada programada quedará cancelada.',
                                )
                              }
                            />
                          )}
                        </div>
                      </div>
                    </li>
                  ))}
              </ul>
            )}
          </Seccion>
        )}

        {pestana === 'cosecha' && (
          <Seccion
            titulo="Cosecha y trazabilidad del grano"
            descripcion="Cada jornada avanza por los estados post-cosecha; el grano nunca retrocede."
            accion={<BotonNuevo onClick={() => setModal('recoleccion')}>Registrar recolección</BotonNuevo>}
            consulta={recolecciones}
            vacio={<Vacio icono={Weight} titulo="Sin recolecciones" texto="Registra la primera jornada de cosecha del lote." />}
          >
            {(datos) => (
              <>
                <div className="mini-indicadores">
                  <div>
                    <span>Café en cereza</span>
                    <strong>{num(datos.reduce((s, x) => s + x.pesoCascaraKg, 0))} kg</strong>
                  </div>
                  <div>
                    <span>Pago a recolectores</span>
                    <strong>{pesos(datos.reduce((s, x) => s + x.costoManoObra, 0))}</strong>
                  </div>
                  <div>
                    <span>Jornadas</span>
                    <strong>{datos.length}</strong>
                  </div>
                </div>
                <ul className="lista lista--tarjetas">
                  {datos.map((x) => (
                    <li key={x.id} className="recoleccion">
                      <div className="recoleccion__cabecera">
                        <div className="lista__texto">
                          <strong>
                            {num(x.pesoCascaraKg)} kg · {fecha(x.fecha)}
                          </strong>
                          <span>
                            {x.recolector || 'Sin recolector'} · {pesos(x.valorPorKgArroba)}/kg · pago {pesos(x.costoManoObra)}
                          </span>
                        </div>
                        <BotonAnular onClick={() => confirmarAnulacion('recolección', () => anularRecol.ejecutar(x.id))} />
                      </div>
                      <PasosGrano
                        recoleccion={x}
                        alAvanzar={(destino) =>
                          setPendiente({
                            titulo: `Avanzar a "${ESTADOS_GRANO[destino]}"`,
                            mensaje: 'La trazabilidad del grano solo avanza: después no podrás volver al estado anterior.',
                            textoAccion: 'Avanzar',
                            ejecutar: () => avanzarGrano.ejecutar(x.id, destino),
                          })
                        }
                      />
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Seccion>
        )}

        {pestana === 'ventas' && (
          <Seccion
            titulo="Ventas del lote"
            descripcion="Cada venta publica un ingreso que ms-financiero suma al balance."
            accion={<BotonNuevo onClick={() => setModal('venta')}>Registrar venta</BotonNuevo>}
            consulta={ventas}
            vacio={<Vacio icono={HandCoins} titulo="Sin ventas" texto="Cuando vendas café de este lote, regístralo aquí." />}
          >
            {(datos) => (
              <div className="tabla-envoltura">
                <table className="tabla">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Comprador</th>
                      <th className="num">Cantidad</th>
                      <th className="num">Precio / @</th>
                      <th className="num">Total</th>
                      <th aria-label="Acciones" />
                    </tr>
                  </thead>
                  <tbody>
                    {datos.map((x) => (
                      <tr key={x.id}>
                        <td className="nowrap">{fecha(x.fecha)}</td>
                        <td>{x.comprador}</td>
                        <td className="num">{num(x.cantidad)} @</td>
                        <td className="num">{pesos(x.precioPorArroba)}</td>
                        <td className="num">
                          <strong>{pesos(x.montoTotal)}</strong>
                        </td>
                        <td className="acciones-fila">
                          <BotonAnular onClick={() => confirmarAnulacion('venta', () => anularVenta.ejecutar(x.id))} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colSpan={4}>Total vendido</td>
                      <td className="num">{pesos(datos.reduce((s, x) => s + x.montoTotal, 0))}</td>
                      <td />
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}
          </Seccion>
        )}
      </div>

      <FormularioLote abierto={modal === 'editar'} alCerrar={() => setModal(null)} fincaId={l.fincaId} lote={l} />
      <FormularioLabor abierto={modal === 'labor'} alCerrar={() => setModal(null)} loteId={l.id} />
      <FormularioFertilizacion abierto={modal === 'fertilizacion'} alCerrar={() => setModal(null)} loteId={l.id} />
      <FormularioRecoleccion abierto={modal === 'recoleccion'} alCerrar={() => setModal(null)} loteId={l.id} />
      <FormularioVenta abierto={modal === 'venta'} alCerrar={() => setModal(null)} loteId={l.id} />
      <FormularioEjecucion abierto={!!ejecutando} alCerrar={() => setEjecutando(null)} fertilizacion={ejecutando} />
      <Confirmar
        abierto={!!pendiente}
        titulo={pendiente?.titulo ?? ''}
        mensaje={pendiente?.mensaje}
        textoAccion={pendiente?.textoAccion}
        peligro={pendiente?.peligro}
        ocupado={ocupado}
        alConfirmar={() => pendiente?.ejecutar()}
        alCerrar={cerrarConfirmacion}
      />
    </>
  )
}

function Seccion<T>({
  titulo,
  descripcion,
  accion,
  consulta,
  vacio,
  children,
}: {
  titulo: string
  descripcion: string
  accion: ReactNode
  consulta: { data?: T[]; isLoading: boolean; error: unknown; refetch: () => unknown }
  vacio: ReactNode
  children: (datos: T[]) => ReactNode
}) {
  return (
    <>
      <div className="seccion__cabecera">
        <div>
          <h2>{titulo}</h2>
          <p className="texto-secundario">{descripcion}</p>
        </div>
        {accion}
      </div>
      {consulta.isLoading ? (
        <Cargando />
      ) : consulta.data ? (
        consulta.data.length === 0 ? (
          vacio
        ) : (
          children(consulta.data)
        )
      ) : (
        <ErrorCarga error={consulta.error} reintentar={() => consulta.refetch()} />
      )}
    </>
  )
}

const BotonNuevo = ({ onClick, children }: { onClick: () => void; children: ReactNode }) => (
  <button className="btn btn--primario" onClick={onClick}>
    <Plus size={16} /> {children}
  </button>
)

const BotonAnular = ({ onClick }: { onClick: () => void }) => (
  <button className="icono-btn icono-btn--peligro" onClick={onClick} title="Anular" aria-label="Anular">
    <Trash2 size={16} />
  </button>
)

function ProporcionBalance({ ingresos, costos }: { ingresos: number; costos: number }) {
  const total = ingresos + costos
  if (total <= 0) return null
  return (
    <div className="proporcion" aria-hidden>
      <span className="proporcion__ingresos" style={{ flexGrow: ingresos }} />
      <span className="proporcion__costos" style={{ flexGrow: costos }} />
    </div>
  )
}

function PasosGrano({ recoleccion, alAvanzar }: { recoleccion: Recoleccion; alAvanzar: (destino: EstadoGrano) => void }) {
  const actual = ORDEN_GRANO.indexOf(recoleccion.estadoGrano)
  return (
    <ol className="pasos" aria-label="Estado del grano">
      {ORDEN_GRANO.map((estado, i) => {
        const clase = i < actual ? 'pasos__paso--hecho' : i === actual ? 'pasos__paso--actual' : 'pasos__paso--pendiente'
        return (
          <li key={estado} className={`pasos__paso ${clase}`}>
            {i > actual ? (
              <button type="button" onClick={() => alAvanzar(estado)} title={`Avanzar a ${ESTADOS_GRANO[estado]}`}>
                <span className="pasos__punto" />
                <span className="pasos__texto">{ESTADOS_GRANO[estado]}</span>
              </button>
            ) : (
              <span className="pasos__contenido" aria-current={i === actual ? 'step' : undefined}>
                <span className="pasos__punto">{i < actual && <Check size={11} strokeWidth={3} />}</span>
                <span className="pasos__texto">{ESTADOS_GRANO[estado]}</span>
              </span>
            )}
          </li>
        )
      })}
    </ol>
  )
}
