import { useLayoutEffect, useRef, useState, type RefObject } from 'react'
import { ChartColumn, Table } from 'lucide-react'
import { pesos, pesosCortos } from '../lib/formato'

function useAncho(ref: RefObject<HTMLElement | null>, montado: boolean) {
  const [ancho, setAncho] = useState(0)
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    setAncho(el.clientWidth)
    const observador = new ResizeObserver(([entrada]) => setAncho(entrada.contentRect.width))
    observador.observe(el)
    return () => observador.disconnect()
  }, [ref, montado])
  return ancho
}

/** Maximo "redondo" del eje (1, 2, 2.5, 5 x 10^n) para que las marcas sean legibles. */
function maximoRedondo(valor: number): number {
  if (valor <= 0) return 1
  const potencia = 10 ** Math.floor(Math.log10(valor))
  for (const paso of [1, 2, 2.5, 5, 10]) {
    if (paso * potencia >= valor) return paso * potencia
  }
  return 10 * potencia
}

/** Barra vertical con extremo de datos redondeado (4px) y base recta sobre el eje. */
function barra(x: number, y: number, ancho: number, alto: number): string {
  const r = Math.min(4, ancho / 2, alto)
  return `M${x},${y + alto} V${y + r} Q${x},${y} ${x + r},${y} H${x + ancho - r} Q${x + ancho},${y} ${x + ancho},${y + r} V${y + alto} Z`
}

export interface FilaIngresosCostos {
  id: string
  etiqueta: string
  ingresos: number
  costos: number
}

const SERIES = [
  { clave: 'ingresos', nombre: 'Ingresos', color: 'var(--serie-ingresos)' },
  { clave: 'costos', nombre: 'Costos', color: 'var(--serie-costos)' },
] as const

export function Leyenda() {
  return (
    <div className="leyenda">
      {SERIES.map((s) => (
        <span key={s.clave}>
          <i style={{ background: s.color }} aria-hidden /> {s.nombre}
        </span>
      ))}
    </div>
  )
}

/** Ingresos vs costos por categoria (finca o lote): barras agrupadas, un solo eje. */
export function GraficaIngresosCostos({ datos, titulo }: { datos: FilaIngresosCostos[]; titulo: string }) {
  const contenedor = useRef<HTMLDivElement>(null)
  const [vista, setVista] = useState<'grafica' | 'tabla'>('grafica')
  const ancho = useAncho(contenedor, vista === 'grafica')
  const [activo, setActivo] = useState<number | null>(null)

  const alto = 260
  const margen = { izq: 64, der: 8, sup: 12, inf: 34 }
  const interiorAncho = Math.max(ancho - margen.izq - margen.der, 0)
  const interiorAlto = alto - margen.sup - margen.inf
  const maximo = maximoRedondo(Math.max(...datos.flatMap((d) => [d.ingresos, d.costos]), 0))
  const y = (v: number) => margen.sup + interiorAlto - (Math.max(v, 0) / maximo) * interiorAlto
  const grupo = datos.length ? interiorAncho / datos.length : 0
  const anchoBarra = Math.max(Math.min(34, grupo * 0.3), 6)
  const marcas = [0, 0.25, 0.5, 0.75, 1].map((p) => p * maximo)
  const maxCaracteres = Math.max(Math.floor(grupo / 7.5), 4)
  const fila = activo == null ? null : datos[activo]

  return (
    <figure className="grafica">
      <figcaption className="grafica__cabecera">
        <div>
          <h3>{titulo}</h3>
          <Leyenda />
        </div>
        <div className="segmentado segmentado--mini" role="tablist" aria-label="Vista">
          <button role="tab" aria-selected={vista === 'grafica'} className={vista === 'grafica' ? 'activo' : ''} onClick={() => setVista('grafica')} title="Ver gráfica">
            <ChartColumn size={15} />
          </button>
          <button role="tab" aria-selected={vista === 'tabla'} className={vista === 'tabla' ? 'activo' : ''} onClick={() => setVista('tabla')} title="Ver tabla">
            <Table size={15} />
          </button>
        </div>
      </figcaption>

      {vista === 'tabla' ? (
        <div className="tabla-envoltura">
          <table className="tabla">
            <thead>
              <tr>
                <th>Nombre</th>
                <th className="num">Ingresos</th>
                <th className="num">Costos</th>
                <th className="num">Balance</th>
              </tr>
            </thead>
            <tbody>
              {datos.map((d) => (
                <tr key={d.id}>
                  <td>{d.etiqueta}</td>
                  <td className="num">{pesos(d.ingresos)}</td>
                  <td className="num">{pesos(d.costos)}</td>
                  <td className="num">{pesos(d.ingresos - d.costos)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div ref={contenedor} className="grafica__lienzo" onMouseLeave={() => setActivo(null)}>
          {ancho > 0 && (
            <svg width={ancho} height={alto} role="img" aria-label={`${titulo}. Usa la vista de tabla para ver los valores.`}>
              {marcas.map((m) => (
                <g key={m}>
                  <line x1={margen.izq} x2={ancho - margen.der} y1={y(m)} y2={y(m)} className={m === 0 ? 'eje' : 'rejilla'} />
                  <text x={margen.izq - 10} y={y(m)} dy="0.32em" textAnchor="end" className="marca">
                    {pesosCortos(m)}
                  </text>
                </g>
              ))}
              {datos.map((d, i) => {
                const centro = margen.izq + grupo * i + grupo / 2
                const etiqueta = d.etiqueta.length > maxCaracteres ? d.etiqueta.slice(0, maxCaracteres - 1) + '…' : d.etiqueta
                return (
                  <g key={d.id}>
                    {activo === i && <rect x={centro - grupo / 2 + 4} y={margen.sup} width={grupo - 8} height={interiorAlto} rx={6} className="resalte" />}
                    {SERIES.map((s, j) => {
                      const valor = d[s.clave]
                      const x = j === 0 ? centro - anchoBarra - 1 : centro + 1
                      const altoBarra = margen.sup + interiorAlto - y(valor)
                      return altoBarra > 0 ? <path key={s.clave} d={barra(x, y(valor), anchoBarra, altoBarra)} fill={s.color} /> : null
                    })}
                    <text x={centro} y={alto - 12} textAnchor="middle" className="marca marca--categoria">
                      {etiqueta}
                    </text>
                    <rect
                      x={centro - grupo / 2}
                      y={0}
                      width={grupo}
                      height={alto}
                      fill="transparent"
                      onMouseEnter={() => setActivo(i)}
                      onFocus={() => setActivo(i)}
                      tabIndex={0}
                      aria-label={`${d.etiqueta}: ingresos ${pesos(d.ingresos)}, costos ${pesos(d.costos)}`}
                    />
                  </g>
                )
              })}
            </svg>
          )}
          {fila && activo != null && (
            <div
              className="tooltip"
              style={{
                left: Math.min(Math.max(margen.izq + grupo * activo + grupo / 2, 110), ancho - 110),
                top: Math.max(y(Math.max(fila.ingresos, fila.costos)) - 12, 0),
              }}
            >
              <strong>{fila.etiqueta}</strong>
              {SERIES.map((s) => (
                <span key={s.clave} className="tooltip__fila">
                  <i style={{ background: s.color }} aria-hidden /> {s.nombre}
                  <b>{pesos(fila[s.clave])}</b>
                </span>
              ))}
              <span className="tooltip__fila tooltip__fila--total">
                Balance <b>{pesos(fila.ingresos - fila.costos)}</b>
              </span>
            </div>
          )}
        </div>
      )}
    </figure>
  )
}

export interface FilaBalance {
  id: string
  etiqueta: string
  detalle?: string
  valor: number
}

/** Balance por lote: barras divergentes desde cero (ganancia a la derecha, perdida a la izquierda). */
export function GraficaBalance({ datos, alSeleccionar }: { datos: FilaBalance[]; alSeleccionar?: (id: string) => void }) {
  const maximo = Math.max(...datos.map((d) => Math.abs(d.valor)), 1)
  return (
    <ul className="divergente">
      {datos.map((d) => {
        const proporcion = (Math.abs(d.valor) / maximo) * 50
        const positivo = d.valor >= 0
        return (
          <li key={d.id}>
            <button
              type="button"
              className="divergente__fila"
              onClick={() => alSeleccionar?.(d.id)}
              title={`${d.etiqueta}: ${pesos(d.valor)}`}
            >
              <span className="divergente__etiqueta">
                <strong>{d.etiqueta}</strong>
                {d.detalle && <small>{d.detalle}</small>}
              </span>
              <span className="divergente__pista" aria-hidden>
                <span className="divergente__cero" />
                <span
                  className={`divergente__barra ${positivo ? 'divergente__barra--pos' : 'divergente__barra--neg'}`}
                  style={positivo ? { left: '50%', width: `${proporcion}%` } : { right: '50%', width: `${proporcion}%` }}
                />
              </span>
              <span className={`divergente__valor ${positivo ? '' : 'divergente__valor--neg'}`}>
                {positivo ? '+' : '−'}
                {pesosCortos(Math.abs(d.valor))}
              </span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}
