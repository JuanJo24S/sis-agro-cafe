package com.sisagrodev.cosecha.repository;

import com.sisagrodev.cosecha.domain.Venta;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VentaRepository extends JpaRepository<Venta, UUID>,
		JpaSpecificationExecutor<Venta> {
}
