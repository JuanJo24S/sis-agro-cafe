import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { MapPin, Plus, Ruler, Search, Sprout, Trees } from 'lucide-react'
import { useResumen } from '../api/agrocafe'
import { Cargando, Encabezado, ErrorCarga, Saldo, Vacio } from '../components/ui'
import { FormularioFinca } from '../forms/formularios'
import { num } from '../lib/formato'

const normalizar = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

export function Fincas() {
  const resumen = useResumen()
  const [busqueda, setBusqueda] = useState('')
  const [nueva, setNueva] = useState(false)

  const fincas = useMemo(() => {
    const todas = resumen.data?.content ?? []
    const q = normalizar(busqueda.trim())
    if (!q) return todas
    return todas.filter((f) =>
      normalizar([f.nombre, f.municipio, f.departamento ?? '', ...f.lotes.map((l) => `${l.nombre} ${l.variedad}`)].join(' ')).includes(q),
    )
  }, [resumen.data, busqueda])

  return (
    <>
      <Encabezado
        titulo="Fincas y lotes"
        descripcion="Tus predios cafeteros y las parcelas que los componen."
        acciones={
          <button className="btn btn--primario" onClick={() => setNueva(true)}>
            <Plus size={16} /> Nueva finca
          </button>
        }
      />

      {(resumen.data?.content.length ?? 0) > 0 && (
        <div className="barra-filtros">
          <label className="buscador">
            <Search size={17} aria-hidden />
            <input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar por finca, municipio, lote o variedad…"
              aria-label="Buscar fincas"
            />
          </label>
          <span className="texto-secundario">
            {fincas.length} de {resumen.data?.totalElements ?? 0} fincas
          </span>
        </div>
      )}

      {resumen.isLoading ? (
        <div className="rejilla-tarjetas">
          {[0, 1, 2].map((i) => (
            <div key={i} className="tarjeta">
              <Cargando />
            </div>
          ))}
        </div>
      ) : resumen.isError ? (
        <div className="tarjeta">
          <ErrorCarga error={resumen.error} reintentar={() => resumen.refetch()} />
        </div>
      ) : fincas.length === 0 ? (
        <div className="tarjeta">
          {busqueda ? (
            <Vacio icono={Search} titulo="Sin resultados" texto={`Ninguna finca coincide con "${busqueda}".`} />
          ) : (
            <Vacio
              icono={Trees}
              titulo="Aún no tienes fincas"
              texto="Registra tu primera finca para empezar a gestionar sus lotes."
              accion={
                <button className="btn btn--primario" onClick={() => setNueva(true)}>
                  <Plus size={16} /> Nueva finca
                </button>
              }
            />
          )}
        </div>
      ) : (
        <div className="rejilla-tarjetas">
          {fincas.map((f) => {
            const areaLotes = f.lotes.reduce((s, l) => s + (l.areaHectareas ?? 0), 0)
            const variedades = [...new Set(f.lotes.map((l) => l.variedad))]
            return (
              <Link key={f.id} to={`/fincas/${f.id}`} className="tarjeta tarjeta--enlace finca">
                <div className="finca__cabecera">
                  <span className="finca__icono">
                    <Trees size={22} aria-hidden />
                  </span>
                  <div>
                    <h3>{f.nombre}</h3>
                    <span className="texto-secundario">
                      <MapPin size={13} aria-hidden /> {f.municipio}
                      {f.departamento ? `, ${f.departamento}` : ''}
                    </span>
                  </div>
                </div>
                <dl className="finca__datos">
                  <div>
                    <dt>
                      <Sprout size={14} aria-hidden /> Lotes
                    </dt>
                    <dd>{f.lotes.length}</dd>
                  </div>
                  <div>
                    <dt>
                      <Ruler size={14} aria-hidden /> Área
                    </dt>
                    <dd>{f.areaTotalHectareas != null ? `${num(f.areaTotalHectareas)} ha` : `${num(areaLotes)} ha`}</dd>
                  </div>
                </dl>
                {variedades.length > 0 && (
                  <div className="etiquetas">
                    {variedades.slice(0, 4).map((v) => (
                      <span key={v} className="etiqueta">
                        {v}
                      </span>
                    ))}
                  </div>
                )}
                <div className="finca__pie">
                  <span className="texto-secundario">Balance</span>
                  {f.balance ? <Saldo valor={f.balance.balance} /> : <span className="texto-tenue">Sin movimientos</span>}
                </div>
              </Link>
            )
          })}
        </div>
      )}

      <FormularioFinca abierto={nueva} alCerrar={() => setNueva(false)} />
    </>
  )
}
