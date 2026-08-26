package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.Lote;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

	Page<Lote> findByFincaId(UUID fincaId, Pageable paginacion);

	List<Lote> findByIdInAndFincaPropietarioId(Collection<UUID> ids, UUID propietarioId);
}
