package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.Finca;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FincaRepository extends JpaRepository<Finca, UUID> {

	Page<Finca> findByPropietarioId(UUID propietarioId, Pageable paginacion);
}
