package com.sisagrodev.agronomico.service;

import com.sisagrodev.agronomico.domain.Finca;
import com.sisagrodev.agronomico.error.ExcepcionNegocio;
import com.sisagrodev.agronomico.repository.FincaRepository;
import com.sisagrodev.agronomico.web.dto.FincaRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FincaService {

	private final FincaRepository repositorio;

	public FincaService(FincaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Transactional
	public Finca crear(UUID propietarioId, FincaRequest solicitud) {
		Finca finca = new Finca(propietarioId);
		copiar(solicitud, finca);
		return repositorio.save(finca);
	}

	@Transactional(readOnly = true)
	public Page<Finca> listar(UUID propietarioId, Pageable paginacion) {
		return repositorio.findByPropietarioId(propietarioId, paginacion);
	}

	/** Devuelve la finca solo si pertenece al usuario autenticado (404 vs 403). */
	@Transactional(readOnly = true)
	public Finca delUsuario(UUID fincaId, UUID propietarioId) {
		Finca finca = repositorio.findById(fincaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La finca %s no existe.".formatted(fincaId)));
		if (!finca.getPropietarioId().equals(propietarioId)) {
			throw ExcepcionNegocio.prohibido("La finca no pertenece al usuario autenticado.");
		}
		return finca;
	}

	@Transactional
	public Finca actualizar(UUID fincaId, UUID propietarioId, FincaRequest solicitud) {
		Finca finca = delUsuario(fincaId, propietarioId);
		copiar(solicitud, finca);
		return finca;
	}

	private void copiar(FincaRequest solicitud, Finca finca) {
		finca.setNombre(solicitud.nombre());
		finca.setMunicipio(solicitud.municipio());
		finca.setDepartamento(solicitud.departamento());
		finca.setAreaTotalHectareas(solicitud.areaTotalHectareas());
	}
}
