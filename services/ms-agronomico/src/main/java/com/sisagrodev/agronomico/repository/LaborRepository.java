package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.Labor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LaborRepository extends JpaRepository<Labor, UUID>,
		JpaSpecificationExecutor<Labor> {
}
