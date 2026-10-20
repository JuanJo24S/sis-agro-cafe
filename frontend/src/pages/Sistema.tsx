import { useQuery } from '@tanstack/react-query'
import { Activity, Database, ExternalLink, Gauge, Network, RefreshCw, Workflow, type LucideIcon } from 'lucide-react'
import { rest, type Servicio } from '../api/http'
import { Encabezado } from '../components/ui'

declare const __PUERTOS_CONFIG__: Record<string, number> | undefined

interface Salud {
  status: string
  components?: Record<string, { status: string }>
}

const PUERTOS_DEFECTO: Record<string, number> = {
  gateway: 18080,
  auth: 18081,
  agronomico: 18082,
  cosecha: 18083,
  financiero: 18084,
  grafana: 13000,
  jaeger: 18686,
  prometheus: 19090,
  kafkaUi: 18085,
}

export function puertoPublicado(clave: string, puertoInternoDefault: number): number {
  if (typeof __PUERTOS_CONFIG__ !== 'undefined' && __PUERTOS_CONFIG__[clave]) {
    return __PUERTOS_CONFIG__[clave]
  }
  const p = window.location.port
  if (!p || p === '15173' || p.startsWith('15') || p.startsWith('18')) {
    return PUERTOS_DEFECTO[clave] ?? puertoInternoDefault
  }
  return puertoInternoDefault
}

const SERVICIOS: { id: Servicio; clave: string; nombre: string; puertoInterno: number; rol: string; swagger?: string }[] = [
  { id: 'gateway', clave: 'gateway', nombre: 'Gateway GraphQL', puertoInterno: 8080, rol: 'BFF: une los microservicios en un solo endpoint' },
  { id: 'auth', clave: 'auth', nombre: 'ms-auth', puertoInterno: 8081, rol: 'Usuarios, registro y emisión de JWT', swagger: '/swagger-ui.html' },
  { id: 'agronomico', clave: 'agronomico', nombre: 'ms-agronómico', puertoInterno: 8082, rol: 'Fincas, lotes, labores y fertilización', swagger: '/swagger-ui.html' },
  { id: 'cosecha', clave: 'cosecha', nombre: 'ms-cosecha', puertoInterno: 8083, rol: 'Recolecciones, trazabilidad y ventas', swagger: '/swagger-ui.html' },
  { id: 'financiero', clave: 'financiero', nombre: 'ms-financiero', puertoInterno: 8084, rol: 'Balance por lote desde eventos Kafka', swagger: '/swagger-ui.html' },
]

const HERRAMIENTAS: { clave: string; nombre: string; puertoInterno: number; ruta: string; icono: LucideIcon; texto: string }[] = [
  { clave: 'gateway', nombre: 'GraphiQL', puertoInterno: 8080, ruta: '/graphiql', icono: Network, texto: 'Explorar el esquema del gateway' },
  { clave: 'grafana', nombre: 'Grafana', puertoInterno: 3000, ruta: '/', icono: Gauge, texto: 'Tablero de microservicios (admin/admin)' },
  { clave: 'jaeger', nombre: 'Jaeger', puertoInterno: 16686, ruta: '/', icono: Workflow, texto: 'Trazas distribuidas de cada petición' },
  { clave: 'prometheus', nombre: 'Prometheus', puertoInterno: 9090, ruta: '/targets', icono: Activity, texto: 'Métricas y targets' },
  { clave: 'kafkaUi', nombre: 'Kafka UI', puertoInterno: 8085, ruta: '/', icono: Database, texto: 'Tópicos y mensajes de eventos' },
]

const COMPONENTES: Record<string, string> = {
  db: 'Base de datos',
  kafka: 'Kafka',
  diskSpace: 'Disco',
  ping: 'Ping',
}

const url = (clave: string, puertoInterno: number, ruta = '/') =>
  `${window.location.protocol}//${window.location.hostname}:${puertoPublicado(clave, puertoInterno)}${ruta}`

function useSalud(servicio: Servicio) {
  return useQuery({
    queryKey: ['salud', servicio],
    queryFn: () => rest<Salud>(servicio, '/actuator/health', { anonimo: true }),
    refetchInterval: 10_000,
    retry: false,
  })
}

type Estado = 'arriba' | 'caido' | 'cargando'

function estadoDe(consulta: ReturnType<typeof useSalud>): Estado {
  if (consulta.isLoading) return 'cargando'
  return consulta.data?.status === 'UP' ? 'arriba' : 'caido'
}

const TEXTO_ESTADO: Record<Estado, string> = { arriba: 'En línea', caido: 'Sin respuesta', cargando: 'Consultando…' }

export function Sistema() {
  const consultas = {
    gateway: useSalud('gateway'),
    auth: useSalud('auth'),
    agronomico: useSalud('agronomico'),
    cosecha: useSalud('cosecha'),
    financiero: useSalud('financiero'),
  }
  const estados = Object.fromEntries(Object.entries(consultas).map(([k, c]) => [k, estadoDe(c)])) as Record<Servicio, Estado>
  const arriba = Object.values(estados).filter((e) => e === 'arriba').length

  return (
    <>
      <Encabezado
        titulo="Estado del sistema"
        descripcion={`${arriba} de ${SERVICIOS.length} servicios en línea · se actualiza cada 10 s`}
        acciones={
          <button className="btn btn--secundario" onClick={() => Object.values(consultas).forEach((c) => c.refetch())}>
            <RefreshCw size={16} /> Actualizar
          </button>
        }
      />

      <div className="tarjeta">
        <div className="tarjeta__cabecera">
          <h2>Arquitectura</h2>
        </div>
        <div className="arquitectura" aria-label="Diagrama de arquitectura">
          <div className="arq-nodo arq-nodo--cliente">
            <img src="/logo-agrocafe-icon.png" alt="" width={26} height={26} />
            <span>
              <strong>Frontend React</strong>
              <small>esta aplicación</small>
            </span>
          </div>
          <div className="arq-flecha" aria-hidden>
            <span>GraphQL</span>
          </div>
          <NodoArq estado={estados.gateway} titulo="Gateway GraphQL" detalle={`:${puertoPublicado('gateway', 8080)} · BFF`} ancho />
          <div className="arq-flecha" aria-hidden>
            <span>REST + JWT</span>
          </div>
          <div className="arq-fila">
            <NodoArq estado={estados.auth} titulo="ms-auth" detalle={`:${puertoPublicado('auth', 8081)}`} />
            <NodoArq estado={estados.agronomico} titulo="ms-agronómico" detalle={`:${puertoPublicado('agronomico', 8082)}`} />
            <NodoArq estado={estados.cosecha} titulo="ms-cosecha" detalle={`:${puertoPublicado('cosecha', 8083)}`} />
            <NodoArq estado={estados.financiero} titulo="ms-financiero" detalle={`:${puertoPublicado('financiero', 8084)}`} />
          </div>
          <div className="arq-bus">
            <Workflow size={16} aria-hidden />
            <span>
              <strong>Kafka</strong> · agronómico y cosecha publican costos e ingresos → ms-financiero consolida el balance
            </span>
          </div>
        </div>
      </div>

      <div className="rejilla-tarjetas">
        {SERVICIOS.map((s) => {
          const consulta = consultas[s.id]
          const estado = estados[s.id]
          const componentes = Object.entries(consulta.data?.components ?? {}).filter(([k]) => k in COMPONENTES)
          return (
            <div key={s.id} className="tarjeta servicio">
              <div className="servicio__cabecera">
                <span className={`semaforo semaforo--${estado}`} aria-hidden />
                <div>
                  <h3>{s.nombre}</h3>
                  <span className={`servicio__estado servicio__estado--${estado}`}>{TEXTO_ESTADO[estado]}</span>
                </div>
              </div>
              <p className="texto-secundario">{s.rol}</p>
              {componentes.length > 0 && (
                <div className="etiquetas">
                  {componentes.map(([k, c]) => (
                    <span key={k} className={`etiqueta ${c.status === 'UP' ? 'etiqueta--ok' : 'etiqueta--mal'}`}>
                      {COMPONENTES[k]} {c.status === 'UP' ? '✓' : '✕'}
                    </span>
                  ))}
                </div>
              )}
              <div className="servicio__enlaces">
                {s.swagger && (
                  <a href={url(s.clave, s.puertoInterno, s.swagger)} target="_blank" rel="noreferrer" className="enlace">
                    Swagger UI <ExternalLink size={13} />
                  </a>
                )}
                {s.id === 'gateway' && (
                  <a href={url('gateway', 8080, '/graphiql')} target="_blank" rel="noreferrer" className="enlace">
                    GraphiQL <ExternalLink size={13} />
                  </a>
                )}
              </div>
            </div>
          )
        })}
      </div>

      <div className="tarjeta__cabecera tarjeta__cabecera--suelta">
        <h2>Observabilidad y herramientas</h2>
      </div>
      <div className="rejilla-herramientas">
        {HERRAMIENTAS.map(({ clave, nombre, puertoInterno, ruta, icono: Icono, texto }) => (
          <a key={nombre} className="tarjeta tarjeta--enlace herramienta" href={url(clave, puertoInterno, ruta)} target="_blank" rel="noreferrer">
            <span className="lista__icono">
              <Icono size={18} aria-hidden />
            </span>
            <span className="lista__texto">
              <strong>
                {nombre} <ExternalLink size={13} aria-hidden />
              </strong>
              <span>{texto}</span>
            </span>
          </a>
        ))}
      </div>
    </>
  )
}

function NodoArq({ estado, titulo, detalle, ancho }: { estado: Estado; titulo: string; detalle: string; ancho?: boolean }) {
  return (
    <div className={`arq-nodo ${ancho ? 'arq-nodo--ancho' : ''} arq-nodo--${estado}`}>
      <span className={`semaforo semaforo--${estado}`} aria-label={TEXTO_ESTADO[estado]} />
      <span>
        <strong>{titulo}</strong>
        <small>{detalle}</small>
      </span>
    </div>
  )
}
