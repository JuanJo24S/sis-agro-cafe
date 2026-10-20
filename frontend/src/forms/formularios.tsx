import { useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Bug, Leaf, Scissors, ShieldCheck, Sparkles, Sprout, Wrench } from 'lucide-react'
import { mutaciones, useResumen, type LoteConBalance } from '../api/agrocafe'
import type { EstadoGrano, Fertilizacion, Finca, Lote, TipoLabor } from '../api/types'
import { BotonEnviar, Campo, ErrorFormulario, Modal } from '../components/ui'
import { ESTADOS_GRANO, ORDEN_GRANO, TIPOS_LABOR, hoyISO, pesos } from '../lib/formato'
import { nuevaClave } from '../lib/sesion'
import { aNumero, errorDeCampo, useAccion, useFormulario } from '../lib/useAccion'

interface PropsModal {
  abierto: boolean
  alCerrar: () => void
}

const texto = (v: unknown) => (v == null ? '' : String(v))

function PieFormulario({ alCerrar, ocupado, accion, nota }: { alCerrar: () => void; ocupado: boolean; accion: string; nota?: ReactNode }) {
  return (
    <footer className="modal__pie">
      {nota && <span className="modal__nota">{nota}</span>}
      <button type="button" className="btn btn--fantasma" onClick={alCerrar} disabled={ocupado}>
        Cancelar
      </button>
      <BotonEnviar ocupado={ocupado}>{accion}</BotonEnviar>
    </footer>
  )
}

/** Indica al publico que el envio lleva Idempotency-Key: un doble clic no duplica el registro. */
const NotaIdempotencia = () => (
  <>
    <ShieldCheck size={14} aria-hidden /> Protegido contra doble envío
  </>
)

function Total({ etiqueta, valor }: { etiqueta: string; valor: number }) {
  return (
    <div className="total-vivo campo--completo">
      <span>{etiqueta}</span>
      <strong>{pesos(valor)}</strong>
    </div>
  )
}

// ------------------------------------------------------------------ Finca

export function FormularioFinca({ abierto, alCerrar, finca }: PropsModal & { finca?: Finca }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo={finca ? 'Editar finca' : 'Nueva finca'}
      subtitulo="Datos generales del predio cafetero"
    >
      <CuerpoFinca alCerrar={alCerrar} finca={finca} />
    </Modal>
  )
}

function CuerpoFinca({ alCerrar, finca }: { alCerrar: () => void; finca?: Finca }) {
  const f = useFormulario({
    nombre: texto(finca?.nombre),
    municipio: texto(finca?.municipio),
    departamento: texto(finca?.departamento ?? 'Huila'),
    areaTotalHectareas: texto(finca?.areaTotalHectareas),
  })
  const accion = useAccion(
    (datos: Record<string, unknown>) =>
      finca ? mutaciones.actualizarFinca(finca.id, datos) : mutaciones.crearFinca(datos),
    { exito: (r) => (finca ? `Finca "${r.nombre}" actualizada` : `Finca "${r.nombre}" registrada`), alTerminar: alCerrar },
  )
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({ ...f.valores, areaTotalHectareas: aNumero(f.valores.areaTotalHectareas) })
  }
  const err = (c: string) => errorDeCampo(accion.erroresCampo, c)
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form">
        <ErrorFormulario mensaje={accion.error} />
        <Campo etiqueta="Nombre de la finca *" ancho="completo" error={err('nombre')}>
          <input {...f.enlazar('nombre')} required maxLength={120} placeholder="Ej. La Esperanza" autoFocus />
        </Campo>
        <Campo etiqueta="Municipio *" error={err('municipio')}>
          <input {...f.enlazar('municipio')} required maxLength={120} placeholder="Ej. Pitalito" />
        </Campo>
        <Campo etiqueta="Departamento" error={err('departamento')}>
          <input {...f.enlazar('departamento')} maxLength={120} placeholder="Ej. Huila" />
        </Campo>
        <Campo etiqueta="Área total (ha)" error={err('areaTotalHectareas')}>
          <input {...f.enlazar('areaTotalHectareas')} type="number" min={0} step="0.01" placeholder="0,00" />
        </Campo>
      </div>
      <PieFormulario alCerrar={alCerrar} ocupado={accion.ocupado} accion={finca ? 'Guardar cambios' : 'Registrar finca'} />
    </form>
  )
}

// ------------------------------------------------------------------ Lote

const VARIEDADES = ['Castillo', 'Colombia', 'Caturra', 'Cenicafé 1', 'Tabi', 'Típica', 'Borbón', 'Geisha']

export function FormularioLote({ abierto, alCerrar, fincaId, lote }: PropsModal & { fincaId: string; lote?: Lote }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo={lote ? 'Editar lote' : 'Nuevo lote'}
      subtitulo="Caracterización agronómica de la parcela"
      ancho="lg"
    >
      <CuerpoLote alCerrar={alCerrar} fincaId={fincaId} lote={lote} />
    </Modal>
  )
}

function CuerpoLote({ alCerrar, fincaId, lote }: { alCerrar: () => void; fincaId: string; lote?: Lote }) {
  const f = useFormulario({
    nombre: texto(lote?.nombre),
    variedad: texto(lote?.variedad ?? 'Castillo'),
    fechaSiembra: texto(lote?.fechaSiembra),
    areaHectareas: texto(lote?.areaHectareas),
    densidadSiembra: texto(lote?.densidadSiembra),
    alturaMsnm: texto(lote?.alturaMsnm),
    phSuelo: texto(lote?.phSuelo),
    tipoSuelo: texto(lote?.tipoSuelo),
    temperaturaPromedio: texto(lote?.temperaturaPromedio),
  })
  const accion = useAccion(
    (datos: Record<string, unknown>) =>
      lote ? mutaciones.actualizarLote(lote.id, datos) : mutaciones.crearLote(fincaId, datos),
    { exito: (r) => (lote ? `Lote "${r.nombre}" actualizado` : `Lote "${r.nombre}" creado`), alTerminar: alCerrar },
  )
  const v = f.valores
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({
      ...v,
      areaHectareas: aNumero(v.areaHectareas),
      densidadSiembra: aNumero(v.densidadSiembra),
      alturaMsnm: aNumero(v.alturaMsnm),
      phSuelo: aNumero(v.phSuelo),
      temperaturaPromedio: aNumero(v.temperaturaPromedio),
    })
  }
  const err = (c: string) => errorDeCampo(accion.erroresCampo, c)
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form rejilla-form--3">
        <ErrorFormulario mensaje={accion.error} />
        <p className="seccion-form campo--completo">Identificación</p>
        <Campo etiqueta="Nombre del lote *" error={err('nombre')}>
          <input {...f.enlazar('nombre')} required maxLength={120} placeholder="Ej. El Mirador" autoFocus />
        </Campo>
        <Campo etiqueta="Variedad *" error={err('variedad')}>
          <input {...f.enlazar('variedad')} required maxLength={80} list="variedades" />
          <datalist id="variedades">
            {VARIEDADES.map((x) => (
              <option key={x} value={x} />
            ))}
          </datalist>
        </Campo>
        <Campo etiqueta="Fecha de siembra *" error={err('fechaSiembra')}>
          <input {...f.enlazar('fechaSiembra')} type="date" required max={hoyISO()} />
        </Campo>
        <Campo etiqueta="Área (ha) *" error={err('areaHectareas')}>
          <input {...f.enlazar('areaHectareas')} type="number" min={0} step="0.01" required />
        </Campo>
        <Campo etiqueta="Densidad (árboles/ha)" error={err('densidadSiembra')}>
          <input {...f.enlazar('densidadSiembra')} type="number" min={1} step={1} placeholder="Ej. 5000" />
        </Campo>
        <Campo etiqueta="Altura (m s.n.m.)" error={err('alturaMsnm')}>
          <input {...f.enlazar('alturaMsnm')} type="number" step={1} placeholder="Ej. 1650" />
        </Campo>
        <p className="seccion-form campo--completo">Suelo y clima</p>
        <Campo etiqueta="pH del suelo" ayuda="Entre 0 y 14" error={err('phSuelo')}>
          <input {...f.enlazar('phSuelo')} type="number" min={0} max={14} step="0.1" placeholder="Ej. 5,5" />
        </Campo>
        <Campo etiqueta="Tipo de suelo" error={err('tipoSuelo')}>
          <input {...f.enlazar('tipoSuelo')} maxLength={120} placeholder="Ej. Franco arcilloso" />
        </Campo>
        <Campo etiqueta="Temperatura promedio (°C)" error={err('temperaturaPromedio')}>
          <input {...f.enlazar('temperaturaPromedio')} type="number" step="0.1" placeholder="Ej. 19,5" />
        </Campo>
      </div>
      <PieFormulario alCerrar={alCerrar} ocupado={accion.ocupado} accion={lote ? 'Guardar cambios' : 'Crear lote'} />
    </form>
  )
}

// ------------------------------------------------------------------ Labor

export const ICONOS_LABOR: Record<TipoLabor, typeof Leaf> = {
  LIMPIA: Sparkles,
  FUMIGACION: Bug,
  DESHIERBE: Sprout,
  PODA: Scissors,
  OTRA: Wrench,
}

export function FormularioLabor({ abierto, alCerrar, loteId }: PropsModal & { loteId: string }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Registrar labor" subtitulo="Labor cultural ejecutada en el lote; su costo se envía a finanzas">
      <CuerpoLabor alCerrar={alCerrar} loteId={loteId} />
    </Modal>
  )
}

function CuerpoLabor({ alCerrar, loteId }: { alCerrar: () => void; loteId: string }) {
  const clave = useMemo(nuevaClave, [])
  const f = useFormulario({
    tipo: 'LIMPIA',
    fechaEjecucion: hoyISO(),
    descripcion: '',
    jornales: '',
    costoManoObra: '',
    costoInsumos: '',
  })
  const v = f.valores
  const accion = useAccion(
    (datos: Record<string, unknown>) => mutaciones.registrarLabor(loteId, datos, clave),
    { exito: 'Labor registrada', detalle: 'El costo se reflejará en el balance en unos segundos.', alTerminar: alCerrar },
  )
  const total = (aNumero(v.costoManoObra) ?? 0) + (aNumero(v.costoInsumos) ?? 0)
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({
      ...v,
      jornales: aNumero(v.jornales),
      costoManoObra: aNumero(v.costoManoObra),
      costoInsumos: aNumero(v.costoInsumos),
    })
  }
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form">
        <ErrorFormulario mensaje={accion.error} />
        <fieldset className="campo campo--completo">
          <legend className="campo__etiqueta">Tipo de labor *</legend>
          <div className="selector-chips">
            {(Object.keys(TIPOS_LABOR) as TipoLabor[]).map((tipo) => {
              const Icono = ICONOS_LABOR[tipo]
              return (
                <button
                  type="button"
                  key={tipo}
                  className={`chip ${v.tipo === tipo ? 'chip--activo' : ''}`}
                  aria-pressed={v.tipo === tipo}
                  onClick={() => f.fijar('tipo', tipo)}
                >
                  <Icono size={15} aria-hidden /> {TIPOS_LABOR[tipo]}
                </button>
              )
            })}
          </div>
        </fieldset>
        <Campo etiqueta="Fecha de ejecución *">
          <input {...f.enlazar('fechaEjecucion')} type="date" required />
        </Campo>
        <Campo etiqueta="Jornales">
          <input {...f.enlazar('jornales')} type="number" min={0} step="0.5" placeholder="Ej. 3" />
        </Campo>
        <Campo etiqueta="Costo mano de obra (COP) *">
          <input {...f.enlazar('costoManoObra')} type="number" min={0} step={100} required placeholder="Ej. 180000" />
        </Campo>
        <Campo etiqueta="Costo insumos (COP)">
          <input {...f.enlazar('costoInsumos')} type="number" min={0} step={100} placeholder="0" />
        </Campo>
        <Campo etiqueta="Descripción" ancho="completo">
          <textarea {...f.enlazar('descripcion')} maxLength={300} rows={2} placeholder="Detalles de la labor (opcional)" />
        </Campo>
        <Total etiqueta="Costo total de la labor" valor={total} />
      </div>
      <PieFormulario alCerrar={alCerrar} ocupado={accion.ocupado} accion="Registrar labor" nota={<NotaIdempotencia />} />
    </form>
  )
}

// ------------------------------------------------------------------ Fertilizacion

const COMPOSICIONES = ['17-6-18-2', 'DAP 18-46-0', 'Urea 46%', 'KCl 0-0-60', 'Abono orgánico (compost)', 'Cal dolomita']

export function FormularioFertilizacion({ abierto, alCerrar, loteId }: PropsModal & { loteId: string }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Fertilización" subtitulo="Registra una abonada realizada o prográmala para después">
      <CuerpoFertilizacion alCerrar={alCerrar} loteId={loteId} />
    </Modal>
  )
}

function CuerpoFertilizacion({ alCerrar, loteId }: { alCerrar: () => void; loteId: string }) {
  const clave = useMemo(nuevaClave, [])
  const f = useFormulario({
    estado: 'EJECUTADA',
    fecha: hoyISO(),
    composicion: '17-6-18-2',
    dosisKgPorArbol: '',
    cantidadTotalKg: '',
    costoInsumos: '',
    costoManoObra: '',
  })
  const v = f.valores
  const programada = v.estado === 'PROGRAMADA'
  const accion = useAccion(
    (datos: Record<string, unknown>) => mutaciones.registrarFertilizacion(loteId, datos, clave),
    {
      exito: programada ? 'Abonada programada' : 'Abonada registrada',
      detalle: programada ? 'Podrás confirmarla cuando se ejecute.' : 'El costo se reflejará en el balance en unos segundos.',
      alTerminar: alCerrar,
    },
  )
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({
      ...v,
      dosisKgPorArbol: aNumero(v.dosisKgPorArbol),
      cantidadTotalKg: aNumero(v.cantidadTotalKg),
      costoInsumos: aNumero(v.costoInsumos),
      costoManoObra: aNumero(v.costoManoObra),
    })
  }
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form">
        <ErrorFormulario mensaje={accion.error} />
        <div className="segmentado campo--completo" role="radiogroup" aria-label="Estado">
          {(['EJECUTADA', 'PROGRAMADA'] as const).map((estado) => (
            <button
              type="button"
              key={estado}
              role="radio"
              aria-checked={v.estado === estado}
              className={v.estado === estado ? 'activo' : ''}
              onClick={() => {
                f.fijar('estado', estado)
                if (estado === 'PROGRAMADA' && v.fecha <= hoyISO()) f.fijar('fecha', hoyISO(15))
                if (estado === 'EJECUTADA' && v.fecha > hoyISO()) f.fijar('fecha', hoyISO())
              }}
            >
              {estado === 'EJECUTADA' ? 'Ya se aplicó' : 'Programar para después'}
            </button>
          ))}
        </div>
        <Campo etiqueta={programada ? 'Fecha programada *' : 'Fecha de aplicación *'}>
          <input {...f.enlazar('fecha')} type="date" required />
        </Campo>
        <Campo etiqueta="Composición *">
          <input {...f.enlazar('composicion')} required maxLength={120} list="composiciones" />
          <datalist id="composiciones">
            {COMPOSICIONES.map((x) => (
              <option key={x} value={x} />
            ))}
          </datalist>
        </Campo>
        <Campo etiqueta="Dosis por árbol (kg) *">
          <input {...f.enlazar('dosisKgPorArbol')} type="number" min={0} step="0.01" required placeholder="Ej. 0,05" />
        </Campo>
        <Campo etiqueta="Cantidad total (kg)">
          <input {...f.enlazar('cantidadTotalKg')} type="number" min={0} step="0.1" placeholder="Ej. 250" />
        </Campo>
        <Campo etiqueta={programada ? 'Costo estimado insumos' : 'Costo insumos (COP)'}>
          <input {...f.enlazar('costoInsumos')} type="number" min={0} step={100} placeholder="0" />
        </Campo>
        <Campo etiqueta={programada ? 'Costo estimado mano de obra' : 'Costo mano de obra (COP)'}>
          <input {...f.enlazar('costoManoObra')} type="number" min={0} step={100} placeholder="0" />
        </Campo>
        {!programada && <Total etiqueta="Costo total" valor={(aNumero(v.costoInsumos) ?? 0) + (aNumero(v.costoManoObra) ?? 0)} />}
      </div>
      <PieFormulario
        alCerrar={alCerrar}
        ocupado={accion.ocupado}
        accion={programada ? 'Programar abonada' : 'Registrar abonada'}
        nota={<NotaIdempotencia />}
      />
    </form>
  )
}

export function FormularioEjecucion({ abierto, alCerrar, fertilizacion }: PropsModal & { fertilizacion: Fertilizacion | null }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Confirmar ejecución" subtitulo={fertilizacion ? `Abonada ${fertilizacion.composicion}` : undefined} ancho="sm">
      {fertilizacion && <CuerpoEjecucion alCerrar={alCerrar} fertilizacion={fertilizacion} />}
    </Modal>
  )
}

function CuerpoEjecucion({ alCerrar, fertilizacion }: { alCerrar: () => void; fertilizacion: Fertilizacion }) {
  const f = useFormulario({
    fechaEjecucion: hoyISO(),
    costoInsumos: texto(fertilizacion.costoInsumos || ''),
    costoManoObra: texto(fertilizacion.costoManoObra || ''),
    observaciones: '',
  })
  const v = f.valores
  const accion = useAccion(
    (datos: Record<string, unknown>) => mutaciones.ejecutarFertilizacion(fertilizacion.id, datos),
    { exito: 'Abonada ejecutada', detalle: 'Su costo ya va camino al balance del lote.', alTerminar: alCerrar },
  )
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({ ...v, costoInsumos: aNumero(v.costoInsumos), costoManoObra: aNumero(v.costoManoObra) })
  }
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form">
        <ErrorFormulario mensaje={accion.error} />
        <Campo etiqueta="Fecha de ejecución *" ancho="completo">
          <input {...f.enlazar('fechaEjecucion')} type="date" required />
        </Campo>
        <Campo etiqueta="Costo insumos (COP)">
          <input {...f.enlazar('costoInsumos')} type="number" min={0} step={100} />
        </Campo>
        <Campo etiqueta="Costo mano de obra (COP)">
          <input {...f.enlazar('costoManoObra')} type="number" min={0} step={100} />
        </Campo>
        <Campo etiqueta="Observaciones" ancho="completo">
          <textarea {...f.enlazar('observaciones')} maxLength={300} rows={2} />
        </Campo>
        <Total etiqueta="Costo total" valor={(aNumero(v.costoInsumos) ?? 0) + (aNumero(v.costoManoObra) ?? 0)} />
      </div>
      <PieFormulario alCerrar={alCerrar} ocupado={accion.ocupado} accion="Confirmar ejecución" />
    </form>
  )
}

// ------------------------------------------------------------------ Recoleccion

export function FormularioRecoleccion({ abierto, alCerrar, loteId }: PropsModal & { loteId: string }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Registrar recolección" subtitulo="Jornada de cosecha y pago a recolectores">
      <CuerpoRecoleccion alCerrar={alCerrar} loteId={loteId} />
    </Modal>
  )
}

function CuerpoRecoleccion({ alCerrar, loteId }: { alCerrar: () => void; loteId: string }) {
  const clave = useMemo(nuevaClave, [])
  const f = useFormulario({
    fecha: hoyISO(),
    pesoCascaraKg: '',
    valorPorKgArroba: '',
    recolector: '',
    estadoGrano: 'CEREZA',
  })
  const v = f.valores
  const accion = useAccion(
    (datos: Record<string, unknown>) => mutaciones.registrarRecoleccion(loteId, datos, clave),
    { exito: 'Recolección registrada', detalle: 'El pago a recolectores se suma a los costos del lote.', alTerminar: alCerrar },
  )
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({ ...v, pesoCascaraKg: aNumero(v.pesoCascaraKg), valorPorKgArroba: aNumero(v.valorPorKgArroba) })
  }
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form">
        <ErrorFormulario mensaje={accion.error} />
        <Campo etiqueta="Fecha *">
          <input {...f.enlazar('fecha')} type="date" required />
        </Campo>
        <Campo etiqueta="Recolector">
          <input {...f.enlazar('recolector')} maxLength={120} placeholder="Nombre o cuadrilla" />
        </Campo>
        <Campo etiqueta="Peso en cereza (kg) *">
          <input {...f.enlazar('pesoCascaraKg')} type="number" min={0} step="0.1" required placeholder="Ej. 320" />
        </Campo>
        <Campo etiqueta="Pago por kg (COP) *">
          <input {...f.enlazar('valorPorKgArroba')} type="number" min={0} step={10} required placeholder="Ej. 900" />
        </Campo>
        <Campo etiqueta="Estado inicial del grano" ancho="completo">
          <select {...f.enlazar('estadoGrano')}>
            {ORDEN_GRANO.map((e: EstadoGrano) => (
              <option key={e} value={e}>
                {ESTADOS_GRANO[e]}
              </option>
            ))}
          </select>
        </Campo>
        <Total etiqueta="Pago a recolectores" valor={(aNumero(v.pesoCascaraKg) ?? 0) * (aNumero(v.valorPorKgArroba) ?? 0)} />
      </div>
      <PieFormulario alCerrar={alCerrar} ocupado={accion.ocupado} accion="Registrar recolección" nota={<NotaIdempotencia />} />
    </form>
  )
}

// ------------------------------------------------------------------ Venta

export function FormularioVenta({ abierto, alCerrar, loteId }: PropsModal & { loteId?: string }) {
  return (
    <Modal abierto={abierto} alCerrar={alCerrar} titulo="Registrar venta" subtitulo="Venta de café; el ingreso se consolida en el balance del lote">
      <CuerpoVenta alCerrar={alCerrar} loteId={loteId} />
    </Modal>
  )
}

function CuerpoVenta({ alCerrar, loteId }: { alCerrar: () => void; loteId?: string }) {
  const clave = useMemo(nuevaClave, [])
  const resumen = useResumen()
  const [lote, setLote] = useState(loteId ?? '')
  const f = useFormulario({ fecha: hoyISO(), cantidad: '', precioPorArroba: '', comprador: '' })
  const v = f.valores
  const accion = useAccion(
    (datos: Record<string, unknown>) => mutaciones.registrarVenta(datos, clave),
    { exito: (r) => `Venta por ${pesos(r.montoTotal)} registrada`, detalle: 'El ingreso llegará al balance vía Kafka.', alTerminar: alCerrar },
  )
  const enviar = (e: FormEvent) => {
    e.preventDefault()
    accion.ejecutar({ ...v, loteId: lote, cantidad: aNumero(v.cantidad), precioPorArroba: aNumero(v.precioPorArroba) })
  }
  const fincas = resumen.data?.content ?? []
  const sinLotes = resumen.isSuccess && fincas.every((x) => x.lotes.length === 0)
  return (
    <form onSubmit={enviar}>
      <div className="modal__cuerpo rejilla-form">
        <ErrorFormulario mensaje={accion.error ?? (sinLotes ? 'Primero crea una finca con al menos un lote.' : null)} />
        <Campo etiqueta="Lote de origen *" ancho="completo">
          <select value={lote} onChange={(e) => setLote(e.target.value)} required disabled={!!loteId}>
            <option value="" disabled>
              {resumen.isLoading ? 'Cargando lotes…' : 'Selecciona un lote'}
            </option>
            {fincas.map((finca) => (
              <optgroup key={finca.id} label={finca.nombre}>
                {finca.lotes.map((l: LoteConBalance) => (
                  <option key={l.id} value={l.id}>
                    {l.nombre} · {l.variedad}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
        </Campo>
        <Campo etiqueta="Fecha *">
          <input {...f.enlazar('fecha')} type="date" required />
        </Campo>
        <Campo etiqueta="Comprador *">
          <input {...f.enlazar('comprador')} required maxLength={160} placeholder="Ej. Cooperativa de Caficultores" list="compradores" />
          <datalist id="compradores">
            <option value="Cooperativa de Caficultores del Huila" />
            <option value="Federación Nacional de Cafeteros" />
            <option value="Trilladora local" />
          </datalist>
        </Campo>
        <Campo etiqueta="Cantidad (arrobas) *" ayuda="1 arroba = 12,5 kg">
          <input {...f.enlazar('cantidad')} type="number" min={0} step="0.1" required placeholder="Ej. 40" />
        </Campo>
        <Campo etiqueta="Precio por arroba (COP) *">
          <input {...f.enlazar('precioPorArroba')} type="number" min={0} step={100} required placeholder="Ej. 180000" />
        </Campo>
        <Total etiqueta="Monto total de la venta" valor={(aNumero(v.cantidad) ?? 0) * (aNumero(v.precioPorArroba) ?? 0)} />
      </div>
      <PieFormulario alCerrar={alCerrar} ocupado={accion.ocupado} accion="Registrar venta" nota={<NotaIdempotencia />} />
    </form>
  )
}
