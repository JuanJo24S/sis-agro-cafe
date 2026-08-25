package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.Lote;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

	Page<Lote> findByFincaId(UUID fincaId, Pageable paginacion);

	@Query("""
			SELECT l FROM Lote l
			WHERE l.finca.id = :fincaId
			  AND (LOWER(l.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
			   OR LOWER(l.variedad) LIKE LOWER(CONCAT('%', :buscar, '%')))
			""")
	Page<Lote> buscarPorFincaId(@Param("fincaId") UUID fincaId,
			@Param("buscar") String buscar, Pageable paginacion);

	List<Lote> findByIdInAndFincaPropietarioId(Collection<UUID> ids, UUID propietarioId);
}
