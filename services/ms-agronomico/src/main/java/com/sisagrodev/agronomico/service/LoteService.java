package com.sisagrodev.agronomico.service;

import com.sisagrodev.agronomico.domain.Finca;
import com.sisagrodev.agronomico.domain.Lote;
import com.sisagrodev.agronomico.error.ExcepcionNegocio;
import com.sisagrodev.agronomico.repository.LoteRepository;
import com.sisagrodev.agronomico.web.dto.LoteRequest;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoteService {

	private final LoteRepository repositorio;
	private final FincaService fincaService;

	public LoteService(LoteRepository repositorio, FincaService fincaService) {
		this.repositorio = repositorio;
		this.fincaService = fincaService;
	}

	@Transactional
	public Lote crear(UUID fincaId, UUID propietarioId, LoteRequest solicitud) {
		Finca finca = fincaService.delUsuario(fincaId, propietarioId);
		Lote lote = new Lote(finca);
		copiar(solicitud, lote);
		return repositorio.save(lote);
	}

	@Transactional(readOnly = true)
	public Page<Lote> listarPorFinca(UUID fincaId, UUID propietarioId, Pageable paginacion) {
		return listarPorFinca(fincaId, propietarioId, null, paginacion);
	}

	@Transactional(readOnly = true)
	public Page<Lote> listarPorFinca(UUID fincaId, UUID propietarioId, String buscar, Pageable paginacion) {
		fincaService.delUsuario(fincaId, propietarioId);
		if (buscar != null && !buscar.isBlank()) {
			return repositorio.buscarPorFincaId(fincaId, buscar.trim(), paginacion);
		}
		return repositorio.findByFincaId(fincaId, paginacion);
	}

	@Transactional(readOnly = true)
	public Lote delUsuario(UUID loteId, UUID propietarioId) {
		Lote lote = repositorio.findById(loteId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El lote %s no existe.".formatted(loteId)));
		if (!lote.getFinca().getPropietarioId().equals(propietarioId)) {
			throw ExcepcionNegocio.prohibido("El lote no pertenece al usuario autenticado.");
		}
		return lote;
	}

	/** Consulta por lotes (batch) que usa el Gateway GraphQL para evitar el N+1. */
	@Transactional(readOnly = true)
	public List<Lote> porIds(Collection<UUID> ids, UUID propietarioId) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return repositorio.findByIdInAndFincaPropietarioId(ids, propietarioId);
	}

	@Transactional
	public Lote actualizar(UUID loteId, UUID propietarioId, LoteRequest solicitud) {
		Lote lote = delUsuario(loteId, propietarioId);
		copiar(solicitud, lote);
		return lote;
	}

	private void copiar(LoteRequest solicitud, Lote lote) {
		lote.setNombre(solicitud.nombre());
		lote.setVariedad(solicitud.variedad());
		lote.setFechaSiembra(solicitud.fechaSiembra());
		lote.setAreaHectareas(solicitud.areaHectareas());
		lote.setDensidadSiembra(solicitud.densidadSiembra());
		lote.setAlturaMsnm(solicitud.alturaMsnm());
		lote.setPhSuelo(solicitud.phSuelo());
		lote.setTipoSuelo(solicitud.tipoSuelo());
		lote.setTemperaturaPromedio(solicitud.temperaturaPromedio());
	}
}
