package com.sisagrodev.financiero.repository;

import com.sisagrodev.financiero.domain.BalanceLote;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BalanceLoteRepository extends JpaRepository<BalanceLote, UUID> {

	List<BalanceLote> findByFincaIdAndUsuarioId(UUID fincaId, UUID usuarioId);

	Page<BalanceLote> findByFincaIdAndUsuarioId(UUID fincaId, UUID usuarioId, Pageable paginacion);

	Page<BalanceLote> findByUsuarioId(UUID usuarioId, Pageable paginacion);

	List<BalanceLote> findByLoteIdInAndUsuarioId(Collection<UUID> loteIds, UUID usuarioId);
}
