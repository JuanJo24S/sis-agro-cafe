package com.sisagrodev.agronomico.web.dto;

import com.sisagrodev.agronomico.domain.EstadoFertilizacion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FertilizacionRequest(
		@NotNull EstadoFertilizacion estado,
		@NotNull LocalDate fecha,
		@NotBlank @Size(max = 120) String composicion,
		@NotNull @DecimalMin("0.0") BigDecimal dosisKgPorArbol,
		@DecimalMin("0.0") BigDecimal cantidadTotalKg,
		@DecimalMin("0.0") BigDecimal costoInsumos,
		@DecimalMin("0.0") BigDecimal costoManoObra) {
}
