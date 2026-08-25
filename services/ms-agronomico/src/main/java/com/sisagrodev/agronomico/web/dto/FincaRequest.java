package com.sisagrodev.agronomico.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FincaRequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Size(max = 120) String municipio,
		@Size(max = 120) String departamento,
		@DecimalMin("0.0") BigDecimal areaTotalHectareas) {
}
