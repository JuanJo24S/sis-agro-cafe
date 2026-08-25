package com.sisagrodev.agronomico.service;

import com.sisagrodev.agronomico.domain.Labor;
import com.sisagrodev.agronomico.domain.Lote;
import com.sisagrodev.agronomico.domain.TipoLabor;
import com.sisagrodev.agronomico.error.ExcepcionNegocio;
import com.sisagrodev.agronomico.evento.MovimientoFinancieroEvent;
import com.sisagrodev.agronomico.evento.Operacion;
import com.sisagrodev.agronomico.evento.OrigenMovimiento;
import com.sisagrodev.agronomico.evento.PublicadorMovimientos;
import com.sisagrodev.agronomico.repository.Filtros;
import com.sisagrodev.agronomico.repository.LaborRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bitacora de labores culturales (RF03) y publicacion del costo asociado (RNF01). */
@Service
public class LaborService {

	private static final String RECURSO = "labor";

	private final LaborRepository repositorio;
	private final LoteService loteService;
	private final IdempotenciaService idempotencia;
	private final PublicadorMovimientos publicador;

	public LaborService(LaborRepository repositorio, LoteService loteService,
			IdempotenciaService idempotencia, PublicadorMovimientos publicador) {
		this.repositorio = repositorio;
		this.loteService = loteService;
		this.idempotencia = idempotencia;
		this.publicador = publicador;
	}

	@Transactional
	public Labor registrar(UUID loteId, UUID usuarioId, UUID claveIdempotencia,
			com.sisagrodev.agronomico.web.dto.LaborRequest solicitud) {
		var previa = idempotencia.recursoPrevio(claveIdempotencia, RECURSO);
		if (previa.isPresent()) {
			return obtener(previa.get(), usuarioId);
		}

		Lote lote = loteService.delUsuario(loteId, usuarioId);
		if (solicitud.fechaEjecucion().isBefore(lote.getFechaSiembra())) {
			throw ExcepcionNegocio.reglaDeNegocio(
					"La fecha de ejecucion no puede ser anterior a la fecha de siembra del lote.");
		}

		Labor labor = new Labor(lote);
		labor.setTipo(solicitud.tipo());
		labor.setDescripcion(solicitud.descripcion());
		labor.setFechaEjecucion(solicitud.fechaEjecucion());
		labor.setJornales(solicitud.jornales());
		labor.setCostoManoObra(solicitud.costoManoObra());
		labor.setCostoInsumos(solicitud.costoInsumos());
		labor.recalcularCostoTotal();
		repositorio.save(labor);

		idempotencia.registrar(claveIdempotencia, RECURSO, labor.getId(), usuarioId);
		publicador.publicar(MovimientoFinancieroEvent.costo(Operacion.REGISTRO,
				OrigenMovimiento.LABOR, labor.getId(), lote.getId(), lote.getFinca().getId(),
				usuarioId, labor.getCostoTotal(), labor.getFechaEjecucion()));
		return labor;
	}

	@Transactional(readOnly = true)
	public Page<Labor> listar(UUID loteId, UUID usuarioId, TipoLabor tipo, LocalDate desde,
			LocalDate hasta, Pageable paginacion) {
		return listar(loteId, usuarioId, tipo, desde, hasta, null, paginacion);
	}

	@Transactional(readOnly = true)
	public Page<Labor> listar(UUID loteId, UUID usuarioId, TipoLabor tipo, LocalDate desde,
			LocalDate hasta, String buscar, Pageable paginacion) {
		loteService.delUsuario(loteId, usuarioId);
		return repositorio.findAll(Filtros.labores(loteId, tipo, desde, hasta, buscar), paginacion);
	}

	@Transactional(readOnly = true)
	public Labor obtener(UUID laborId, UUID usuarioId) {
		Labor labor = repositorio.findById(laborId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La labor %s no existe.".formatted(laborId)));
		if (!labor.getLote().getFinca().getPropietarioId().equals(usuarioId)) {
			throw ExcepcionNegocio.prohibido("La labor no pertenece al usuario autenticado.");
		}
		return labor;
	}

	/**
	 * Anulacion logica: conserva el historico y publica el evento compensatorio para
	 * que ms-financiero descuente el costo que ya habia acumulado.
	 */
	@Transactional
	public void anular(UUID laborId, UUID usuarioId) {
		Labor labor = obtener(laborId, usuarioId);
		if (labor.isAnulada()) {
			return;
		}
		labor.setAnulada(true);
		Lote lote = labor.getLote();
		publicador.publicar(MovimientoFinancieroEvent.costo(Operacion.ANULACION,
				OrigenMovimiento.LABOR, labor.getId(), lote.getId(), lote.getFinca().getId(),
				usuarioId, labor.getCostoTotal(), labor.getFechaEjecucion()));
	}
}
