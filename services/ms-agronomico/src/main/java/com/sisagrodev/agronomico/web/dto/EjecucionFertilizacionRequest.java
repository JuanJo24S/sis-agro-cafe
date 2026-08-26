package com.sisagrodev.agronomico.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EjecucionFertilizacionRequest(
		@NotNull LocalDate fechaEjecucion,
		@DecimalMin("0.0") BigDecimal costoInsumos,
		@DecimalMin("0.0") BigDecimal costoManoObra,
		@Size(max = 300) String observaciones) {
}
