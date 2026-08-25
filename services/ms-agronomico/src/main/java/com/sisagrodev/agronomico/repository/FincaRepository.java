package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.Finca;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FincaRepository extends JpaRepository<Finca, UUID> {

	Page<Finca> findByPropietarioId(UUID propietarioId, Pageable paginacion);

	@Query("""
			SELECT f FROM Finca f
			WHERE f.propietarioId = :propietarioId
			  AND (LOWER(f.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
			   OR LOWER(f.municipio) LIKE LOWER(CONCAT('%', :buscar, '%'))
			   OR (f.departamento IS NOT NULL AND LOWER(f.departamento) LIKE LOWER(CONCAT('%', :buscar, '%'))))
			""")
	Page<Finca> buscarPorPropietarioId(@Param("propietarioId") UUID propietarioId,
			@Param("buscar") String buscar, Pageable paginacion);
}
