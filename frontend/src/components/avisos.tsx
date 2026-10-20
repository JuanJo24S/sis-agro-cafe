import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, Info, X } from 'lucide-react'

type Tipo = 'exito' | 'error' | 'info'
interface Aviso {
  id: number
  tipo: Tipo
  titulo: string
  detalle?: string
}

interface ContextoAvisos {
  exito: (titulo: string, detalle?: string) => void
  error: (titulo: string, detalle?: string) => void
  info: (titulo: string, detalle?: string) => void
}

const Contexto = createContext<ContextoAvisos | null>(null)
const ICONOS = { exito: CheckCircle2, error: AlertTriangle, info: Info }

export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])

  const quitar = useCallback((id: number) => setAvisos((a) => a.filter((x) => x.id !== id)), [])
  const agregar = useCallback(
    (tipo: Tipo, titulo: string, detalle?: string) => {
      const id = Date.now() + Math.random()
      setAvisos((a) => [...a.slice(-3), { id, tipo, titulo, detalle }])
      window.setTimeout(() => quitar(id), tipo === 'error' ? 7000 : 4500)
    },
    [quitar],
  )

  const valor = useMemo(
    () => ({
      exito: (t: string, d?: string) => agregar('exito', t, d),
      error: (t: string, d?: string) => agregar('error', t, d),
      info: (t: string, d?: string) => agregar('info', t, d),
    }),
    [agregar],
  )

  return (
    <Contexto.Provider value={valor}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => {
          const Icono = ICONOS[a.tipo]
          return (
            <div key={a.id} className={`aviso aviso--${a.tipo}`}>
              <Icono size={18} className="aviso__icono" aria-hidden />
              <div className="aviso__texto">
                <strong>{a.titulo}</strong>
                {a.detalle && <span>{a.detalle}</span>}
              </div>
              <button className="icono-btn" onClick={() => quitar(a.id)} aria-label="Cerrar aviso">
                <X size={16} />
              </button>
            </div>
          )
        })}
      </div>
    </Contexto.Provider>
  )
}

export function useAvisos(): ContextoAvisos {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useAvisos debe usarse dentro de ProveedorAvisos')
  return contexto
}
