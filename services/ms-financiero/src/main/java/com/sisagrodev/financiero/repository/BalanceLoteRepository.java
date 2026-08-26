package com.sisagrodev.financiero.repository;

import com.sisagrodev.financiero.domain.BalanceLote;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceLoteRepository extends JpaRepository<BalanceLote, UUID> {

	List<BalanceLote> findByFincaIdAndUsuarioId(UUID fincaId, UUID usuarioId);

	List<BalanceLote> findByLoteIdInAndUsuarioId(Collection<UUID> loteIds, UUID usuarioId);
}
