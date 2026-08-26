package com.sisagrodev.agronomico.service;

import com.sisagrodev.agronomico.domain.ClaveIdempotencia;
import com.sisagrodev.agronomico.repository.ClaveIdempotenciaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve la cabecera `Idempotency-Key` de los POST que publican costos. */
@Service
public class IdempotenciaService {

	private final ClaveIdempotenciaRepository repositorio;

	public IdempotenciaService(ClaveIdempotenciaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Transactional(readOnly = true)
	public Optional<UUID> recursoPrevio(UUID clave, String recurso) {
		if (clave == null) {
			return Optional.empty();
		}
		return repositorio.findByClaveAndRecurso(clave, recurso).map(ClaveIdempotencia::getRecursoId);
	}

	@Transactional
	public void registrar(UUID clave, String recurso, UUID recursoId, UUID usuarioId) {
		if (clave == null) {
			return;
		}
		repositorio.save(new ClaveIdempotencia(clave, recurso, recursoId, usuarioId));
	}
}
