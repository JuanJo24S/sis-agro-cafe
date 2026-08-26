package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.ClaveIdempotencia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotencia, UUID> {

	Optional<ClaveIdempotencia> findByClaveAndRecurso(UUID clave, String recurso);
}
