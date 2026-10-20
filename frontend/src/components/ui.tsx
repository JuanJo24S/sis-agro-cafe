import { useEffect, useId, useRef, type ReactNode } from 'react'
import { AlertTriangle, Loader2, RefreshCw, TrendingDown, TrendingUp, X, type LucideIcon } from 'lucide-react'
import { pesos } from '../lib/formato'

// ------------------------------------------------------------------ Modal

export function Modal({
  titulo,
  subtitulo,
  abierto,
  alCerrar,
  children,
  ancho = 'md',
}: {
  titulo: string
  subtitulo?: string
  abierto: boolean
  alCerrar: () => void
  children: ReactNode
  ancho?: 'sm' | 'md' | 'lg'
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const idTitulo = useId()

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (abierto && !dialogo.open) dialogo.showModal()
    if (!abierto && dialogo.open) dialogo.close()
  }, [abierto])

  return (
    <dialog
      ref={ref}
      className={`modal modal--${ancho}`}
      aria-labelledby={idTitulo}
      onCancel={(e) => {
        e.preventDefault()
        alCerrar()
      }}
      onClick={(e) => {
        if (e.target === ref.current) alCerrar()
      }}
    >
      {abierto && (
        <div className="modal__caja">
          <header className="modal__cabecera">
            <div>
              <h2 id={idTitulo}>{titulo}</h2>
              {subtitulo && <p>{subtitulo}</p>}
            </div>
            <button className="icono-btn" onClick={alCerrar} aria-label="Cerrar">
              <X size={20} />
            </button>
          </header>
          {children}
        </div>
      )}
    </dialog>
  )
}

export function Confirmar({
  abierto,
  titulo,
  mensaje,
  textoAccion = 'Confirmar',
  peligro,
  ocupado,
  alConfirmar,
  alCerrar,
}: {
  abierto: boolean
  titulo: string
  mensaje: ReactNode
  textoAccion?: string
  peligro?: boolean
  ocupado?: boolean
  alConfirmar: () => void
  alCerrar: () => void
}) {
  return (
    <Modal titulo={titulo} abierto={abierto} alCerrar={alCerrar} ancho="sm">
      <div className="modal__cuerpo">
        <p className="texto-secundario">{mensaje}</p>
      </div>
      <footer className="modal__pie">
        <button className="btn btn--fantasma" onClick={alCerrar} disabled={ocupado}>
          Cancelar
        </button>
        <button className={`btn ${peligro ? 'btn--peligro' : 'btn--primario'}`} onClick={alConfirmar} disabled={ocupado}>
          {ocupado && <Loader2 size={16} className="girar" />}
          {textoAccion}
        </button>
      </footer>
    </Modal>
  )
}

// ------------------------------------------------------------------ Formularios

export function Campo({
  etiqueta,
  ayuda,
  error,
  children,
  ancho,
}: {
  etiqueta: string
  ayuda?: string
  error?: string
  children: ReactNode
  ancho?: 'completo'
}) {
  return (
    <label className={`campo ${ancho === 'completo' ? 'campo--completo' : ''} ${error ? 'campo--error' : ''}`}>
      <span className="campo__etiqueta">{etiqueta}</span>
      {children}
      {error ? <span className="campo__error">{error}</span> : ayuda && <span className="campo__ayuda">{ayuda}</span>}
    </label>
  )
}

export function BotonEnviar({ ocupado, children }: { ocupado: boolean; children: ReactNode }) {
  return (
    <button type="submit" className="btn btn--primario" disabled={ocupado}>
      {ocupado && <Loader2 size={16} className="girar" />}
      {children}
    </button>
  )
}

export function ErrorFormulario({ mensaje }: { mensaje?: string | null }) {
  if (!mensaje) return null
  return (
    <div className="alerta alerta--error" role="alert">
      <AlertTriangle size={18} aria-hidden />
      <span>{mensaje}</span>
    </div>
  )
}

// ------------------------------------------------------------------ Estados

export function Cargando({ filas = 3 }: { filas?: number }) {
  return (
    <div className="cargando" aria-busy="true" aria-label="Cargando">
      {Array.from({ length: filas }, (_, i) => (
        <div key={i} className="esqueleto" style={{ width: `${92 - i * 14}%` }} />
      ))}
    </div>
  )
}

export function ErrorCarga({ error, reintentar }: { error: unknown; reintentar?: () => void }) {
  const mensaje = error instanceof Error ? error.message : 'No se pudo cargar la información.'
  return (
    <div className="estado estado--error">
      <AlertTriangle size={28} aria-hidden />
      <p>{mensaje}</p>
      {reintentar && (
        <button className="btn btn--secundario btn--sm" onClick={reintentar}>
          <RefreshCw size={14} /> Reintentar
        </button>
      )}
    </div>
  )
}

export function Vacio({
  icono: Icono,
  titulo,
  texto,
  accion,
}: {
  icono: LucideIcon
  titulo: string
  texto?: string
  accion?: ReactNode
}) {
  return (
    <div className="estado">
      <div className="estado__icono">
        <Icono size={26} aria-hidden />
      </div>
      <h3>{titulo}</h3>
      {texto && <p>{texto}</p>}
      {accion}
    </div>
  )
}

// ------------------------------------------------------------------ Presentacion

export function Encabezado({
  titulo,
  descripcion,
  migas,
  acciones,
}: {
  titulo: ReactNode
  descripcion?: ReactNode
  migas?: ReactNode
  acciones?: ReactNode
}) {
  return (
    <header className="encabezado">
      <div className="encabezado__texto">
        {migas && <nav className="migas" aria-label="Ruta">{migas}</nav>}
        <h1>{titulo}</h1>
        {descripcion && <p>{descripcion}</p>}
      </div>
      {acciones && <div className="encabezado__acciones">{acciones}</div>}
    </header>
  )
}

export function Insignia({ tono = 'neutro', children }: { tono?: 'neutro' | 'exito' | 'aviso' | 'peligro' | 'cafe'; children: ReactNode }) {
  return <span className={`insignia insignia--${tono}`}>{children}</span>
}

/** Monto con signo: el color acompaña al icono y al signo, nunca va solo. */
export function Saldo({ valor, grande }: { valor: number; grande?: boolean }) {
  const positivo = valor >= 0
  const Icono = positivo ? TrendingUp : TrendingDown
  return (
    <span className={`saldo ${positivo ? 'saldo--positivo' : 'saldo--negativo'} ${grande ? 'saldo--grande' : ''}`}>
      <Icono size={grande ? 22 : 15} aria-hidden />
      {positivo ? '+' : '−'}
      {pesos(Math.abs(valor))}
    </span>
  )
}

export function Indicador({
  icono: Icono,
  etiqueta,
  valor,
  detalle,
  tono = 'cafe',
}: {
  icono: LucideIcon
  etiqueta: string
  valor: ReactNode
  detalle?: ReactNode
  tono?: 'cafe' | 'verde' | 'caramelo' | 'neutro'
}) {
  return (
    <div className="indicador">
      <div className={`indicador__icono indicador__icono--${tono}`}>
        <Icono size={20} aria-hidden />
      </div>
      <div className="indicador__cuerpo">
        <span className="indicador__etiqueta">{etiqueta}</span>
        <strong className="indicador__valor">{valor}</strong>
        {detalle && <span className="indicador__detalle">{detalle}</span>}
      </div>
    </div>
  )
}

export function Dato({ etiqueta, valor }: { etiqueta: string; valor: ReactNode }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd>{valor ?? '—'}</dd>
    </div>
  )
}
