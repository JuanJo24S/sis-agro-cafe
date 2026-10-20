import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Calendar, ChevronRight, Coins, MapPin, Mountain, Pencil, Plus, Receipt, Ruler, Scale, Sprout } from 'lucide-react'
import { useFinca } from '../api/agrocafe'
import { GraficaIngresosCostos } from '../components/graficas'
import { Cargando, Encabezado, ErrorCarga, Indicador, Saldo, Vacio } from '../components/ui'
import { FormularioFinca, FormularioLote } from '../forms/formularios'
import { edadCultivo, fecha, num, pesos } from '../lib/formato'

export function FincaDetalle() {
  const { fincaId } = useParams()
  const consulta = useFinca(fincaId)
  const [modal, setModal] = useState<'editar' | 'lote' | null>(null)
  const finca = consulta.data

  if (consulta.isLoading) {
    return (
      <div className="tarjeta">
        <Cargando filas={5} />
      </div>
    )
  }
  if (!finca) {
    return (
      <div className="tarjeta">
        <ErrorCarga error={consulta.error} reintentar={() => consulta.refetch()} />
      </div>
    )
  }

  const areaLotes = finca.lotes.reduce((s, l) => s + (l.areaHectareas ?? 0), 0)
  const ocupacion = finca.areaTotalHectareas ? Math.min(areaLotes / finca.areaTotalHectareas, 1) : null

  return (
    <>
      <Encabezado
        migas={
          <>
            <Link to="/fincas">Fincas</Link>
            <ChevronRight size={14} aria-hidden />
            <span>{finca.nombre}</span>
          </>
        }
        titulo={finca.nombre}
        descripcion={
          <span className="en-linea">
            <MapPin size={15} aria-hidden /> {finca.municipio}
            {finca.departamento ? `, ${finca.departamento}` : ''}
            <span className="punto" aria-hidden />
            <Calendar size={15} aria-hidden /> Registrada el {fecha(finca.fechaCreacion)}
          </span>
        }
        acciones={
          <>
            <button className="btn btn--secundario" onClick={() => setModal('editar')}>
              <Pencil size={16} /> Editar
            </button>
            <button className="btn btn--primario" onClick={() => setModal('lote')}>
              <Plus size={16} /> Nuevo lote
            </button>
          </>
        }
      />

      <section className="indicadores">
        <Indicador
          icono={Ruler}
          etiqueta="Área en lotes"
          valor={`${num(areaLotes)} ha`}
          detalle={
            ocupacion != null ? (
              <span className="ocupacion">
                <span className="ocupacion__barra">
                  <span style={{ width: `${ocupacion * 100}%` }} />
                </span>
                de {num(finca.areaTotalHectareas)} ha
              </span>
            ) : (
              'Área total no registrada'
            )
          }
        />
        <Indicador icono={Coins} etiqueta="Ingresos" valor={pesos(finca.balance?.ingresoTotal)} tono="verde" />
        <Indicador icono={Receipt} etiqueta="Costos" valor={pesos(finca.balance?.costoTotal)} tono="caramelo" />
        <Indicador icono={Scale} etiqueta="Balance" valor={<Saldo valor={finca.balance?.balance ?? 0} />} tono="neutro" />
      </section>

      <div className="tarjeta__cabecera tarjeta__cabecera--suelta">
        <h2>Lotes ({finca.lotes.length})</h2>
      </div>

      {finca.lotes.length === 0 ? (
        <div className="tarjeta">
          <Vacio
            icono={Sprout}
            titulo="Esta finca aún no tiene lotes"
            texto="Divide la finca en lotes para registrar labores, cosechas y ventas por parcela."
            accion={
              <button className="btn btn--primario" onClick={() => setModal('lote')}>
                <Plus size={16} /> Crear lote
              </button>
            }
          />
        </div>
      ) : (
        <>
          <div className="rejilla-tarjetas">
            {finca.lotes.map((l) => (
              <Link key={l.id} to={`/lotes/${l.id}`} className="tarjeta tarjeta--enlace lote">
                <div className="lote__cabecera">
                  <div>
                    <h3>{l.nombre}</h3>
                    <span className="etiqueta etiqueta--cafe">{l.variedad}</span>
                  </div>
                  <ChevronRight size={18} className="texto-tenue" aria-hidden />
                </div>
                <dl className="lote__datos">
                  <div>
                    <dt>Área</dt>
                    <dd>{num(l.areaHectareas)} ha</dd>
                  </div>
                  <div>
                    <dt>Edad</dt>
                    <dd>{edadCultivo(l.fechaSiembra)}</dd>
                  </div>
                  <div>
                    <dt>
                      <Mountain size={13} aria-hidden /> Altura
                    </dt>
                    <dd>{l.alturaMsnm ? `${num(l.alturaMsnm)} m` : '—'}</dd>
                  </div>
                  <div>
                    <dt>pH</dt>
                    <dd>{num(l.phSuelo)}</dd>
                  </div>
                </dl>
                <div className="finca__pie">
                  <span className="texto-secundario">Balance</span>
                  {l.balance ? <Saldo valor={l.balance.balance} /> : <span className="texto-tenue">Sin movimientos</span>}
                </div>
              </Link>
            ))}
          </div>

          <div className="tarjeta" style={{ marginTop: 'var(--esp-6)' }}>
            <GraficaIngresosCostos
              titulo="Ingresos vs costos por lote"
              datos={finca.lotes.map((l) => ({
                id: l.id,
                etiqueta: l.nombre,
                ingresos: l.balance?.ingresoTotal ?? 0,
                costos: l.balance?.costoTotal ?? 0,
              }))}
            />
          </div>
        </>
      )}

      <FormularioFinca abierto={modal === 'editar'} alCerrar={() => setModal(null)} finca={finca} />
      <FormularioLote abierto={modal === 'lote'} alCerrar={() => setModal(null)} fincaId={finca.id} />
    </>
  )
}
