package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.Fertilizacion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FertilizacionRepository extends JpaRepository<Fertilizacion, UUID>,
		JpaSpecificationExecutor<Fertilizacion> {
}
