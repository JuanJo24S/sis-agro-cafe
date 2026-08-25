package com.sisagrodev.agronomico.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteRequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Size(max = 80) String variedad,
		@NotNull LocalDate fechaSiembra,
		@NotNull @DecimalMin("0.0") BigDecimal areaHectareas,
		@Positive Integer densidadSiembra,
		Integer alturaMsnm,
		@DecimalMin("0.0") @DecimalMax("14.0") BigDecimal phSuelo,
		@Size(max = 120) String tipoSuelo,
		BigDecimal temperaturaPromedio) {
}
