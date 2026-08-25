package com.sisagrodev.agronomico.web.dto;

import com.sisagrodev.agronomico.domain.TipoLabor;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LaborRequest(
		@NotNull TipoLabor tipo,
		@Size(max = 300) String descripcion,
		@NotNull LocalDate fechaEjecucion,
		@DecimalMin("0.0") BigDecimal jornales,
		@NotNull @DecimalMin("0.0") BigDecimal costoManoObra,
		@DecimalMin("0.0") BigDecimal costoInsumos) {
}
