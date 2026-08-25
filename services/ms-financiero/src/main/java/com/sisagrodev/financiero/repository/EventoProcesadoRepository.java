package com.sisagrodev.financiero.repository;

import com.sisagrodev.financiero.domain.EventoProcesado;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, UUID> {
}
