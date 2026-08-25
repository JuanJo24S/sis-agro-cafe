package com.sisagrodev.agronomico.service;

import com.sisagrodev.agronomico.domain.EstadoFertilizacion;
import com.sisagrodev.agronomico.domain.Fertilizacion;
import com.sisagrodev.agronomico.domain.Lote;
import com.sisagrodev.agronomico.error.ExcepcionNegocio;
import com.sisagrodev.agronomico.evento.MovimientoFinancieroEvent;
import com.sisagrodev.agronomico.evento.Operacion;
import com.sisagrodev.agronomico.evento.OrigenMovimiento;
import com.sisagrodev.agronomico.evento.PublicadorMovimientos;
import com.sisagrodev.agronomico.repository.FertilizacionRepository;
import com.sisagrodev.agronomico.repository.Filtros;
import com.sisagrodev.agronomico.web.dto.EjecucionFertilizacionRequest;
import com.sisagrodev.agronomico.web.dto.FertilizacionRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abonadas (RF04). Una fertilizacion PROGRAMADA es solo plan y no genera costo; el
 * evento `CostoRegistrado` se publica unicamente cuando queda EJECUTADA.
 */
@Service
public class FertilizacionService {

	private static final String RECURSO = "fertilizacion";

	private final FertilizacionRepository repositorio;
	private final LoteService loteService;
	private final IdempotenciaService idempotencia;
	private final PublicadorMovimientos publicador;

	public FertilizacionService(FertilizacionRepository repositorio, LoteService loteService,
			IdempotenciaService idempotencia, PublicadorMovimientos publicador) {
		this.repositorio = repositorio;
		this.loteService = loteService;
		this.idempotencia = idempotencia;
		this.publicador = publicador;
	}

	@Transactional
	public Fertilizacion registrar(UUID loteId, UUID usuarioId, UUID claveIdempotencia,
			FertilizacionRequest solicitud) {
		var previa = idempotencia.recursoPrevio(claveIdempotencia, RECURSO);
		if (previa.isPresent()) {
			return obtener(previa.get(), usuarioId);
		}
		if (solicitud.estado() == EstadoFertilizacion.CANCELADA) {
			throw ExcepcionNegocio.reglaDeNegocio(
					"Una fertilizacion no se puede crear directamente como CANCELADA.");
		}

		Lote lote = loteService.delUsuario(loteId, usuarioId);
		Fertilizacion fertilizacion = new Fertilizacion(lote);
		fertilizacion.setEstado(solicitud.estado());
		fertilizacion.setFecha(solicitud.fecha());
		fertilizacion.setComposicion(solicitud.composicion());
		fertilizacion.setDosisKgPorArbol(solicitud.dosisKgPorArbol());
		fertilizacion.setCantidadTotalKg(solicitud.cantidadTotalKg());
		fertilizacion.setCostoInsumos(solicitud.costoInsumos());
		fertilizacion.setCostoManoObra(solicitud.costoManoObra());
		fertilizacion.recalcularCostoTotal();
		repositorio.save(fertilizacion);

		idempotencia.registrar(claveIdempotencia, RECURSO, fertilizacion.getId(), usuarioId);
		if (fertilizacion.getEstado() == EstadoFertilizacion.EJECUTADA) {
			publicarCosto(fertilizacion, usuarioId, Operacion.REGISTRO);
		}
		return fertilizacion;
	}

	@Transactional(readOnly = true)
	public Page<Fertilizacion> listar(UUID loteId, UUID usuarioId, EstadoFertilizacion estado,
			LocalDate desde, LocalDate hasta, Pageable paginacion) {
		return listar(loteId, usuarioId, estado, desde, hasta, null, paginacion);
	}

	@Transactional(readOnly = true)
	public Page<Fertilizacion> listar(UUID loteId, UUID usuarioId, EstadoFertilizacion estado,
			LocalDate desde, LocalDate hasta, String buscar, Pageable paginacion) {
		loteService.delUsuario(loteId, usuarioId);
		return repositorio.findAll(Filtros.fertilizaciones(loteId, estado, desde, hasta, buscar), paginacion);
	}

	@Transactional(readOnly = true)
	public Fertilizacion obtener(UUID fertilizacionId, UUID usuarioId) {
		Fertilizacion fertilizacion = repositorio.findById(fertilizacionId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado(
						"La fertilizacion %s no existe.".formatted(fertilizacionId)));
		if (!fertilizacion.getLote().getFinca().getPropietarioId().equals(usuarioId)) {
			throw ExcepcionNegocio.prohibido("La fertilizacion no pertenece al usuario autenticado.");
		}
		return fertilizacion;
	}

	/** Transicion PROGRAMADA -> EJECUTADA: fija el costo definitivo y publica el evento. */
	@Transactional
	public Fertilizacion ejecutar(UUID fertilizacionId, UUID usuarioId, UUID claveIdempotencia,
			EjecucionFertilizacionRequest solicitud) {
		Fertilizacion fertilizacion = obtener(fertilizacionId, usuarioId);
		if (fertilizacion.getEstado() == EstadoFertilizacion.EJECUTADA) {
			throw ExcepcionNegocio.conflicto("La fertilizacion ya estaba ejecutada.");
		}
		if (fertilizacion.getEstado() == EstadoFertilizacion.CANCELADA) {
			throw ExcepcionNegocio.conflicto("La fertilizacion esta cancelada.");
		}

		fertilizacion.setEstado(EstadoFertilizacion.EJECUTADA);
		fertilizacion.setFecha(solicitud.fechaEjecucion());
		if (solicitud.costoInsumos() != null) {
			fertilizacion.setCostoInsumos(solicitud.costoInsumos());
		}
		if (solicitud.costoManoObra() != null) {
			fertilizacion.setCostoManoObra(solicitud.costoManoObra());
		}
		fertilizacion.setObservaciones(solicitud.observaciones());
		fertilizacion.recalcularCostoTotal();

		idempotencia.registrar(claveIdempotencia, RECURSO + ":ejecucion", fertilizacion.getId(),
				usuarioId);
		publicarCosto(fertilizacion, usuarioId, Operacion.REGISTRO);
		return fertilizacion;
	}

	/** Anula: si ya habia publicado costo, compensa el balance; si no, solo cancela. */
	@Transactional
	public void anular(UUID fertilizacionId, UUID usuarioId) {
		Fertilizacion fertilizacion = obtener(fertilizacionId, usuarioId);
		if (fertilizacion.getEstado() == EstadoFertilizacion.CANCELADA) {
			return;
		}
		boolean habiaPublicadoCosto = fertilizacion.getEstado() == EstadoFertilizacion.EJECUTADA;
		fertilizacion.setEstado(EstadoFertilizacion.CANCELADA);
		if (habiaPublicadoCosto) {
			publicarCosto(fertilizacion, usuarioId, Operacion.ANULACION);
		}
	}

	private void publicarCosto(Fertilizacion fertilizacion, UUID usuarioId, Operacion operacion) {
		Lote lote = fertilizacion.getLote();
		publicador.publicar(MovimientoFinancieroEvent.costo(operacion,
				OrigenMovimiento.FERTILIZACION, fertilizacion.getId(), lote.getId(),
				lote.getFinca().getId(), usuarioId, fertilizacion.getCostoTotal(),
				fertilizacion.getFecha()));
	}
}
