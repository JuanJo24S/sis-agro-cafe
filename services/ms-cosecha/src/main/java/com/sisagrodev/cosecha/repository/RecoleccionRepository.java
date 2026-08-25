package com.sisagrodev.cosecha.repository;

import com.sisagrodev.cosecha.domain.Recoleccion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecoleccionRepository extends JpaRepository<Recoleccion, UUID>,
		JpaSpecificationExecutor<Recoleccion> {
}
