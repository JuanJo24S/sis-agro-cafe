package com.sisagrodev.cosecha.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VentaRequest(
		@NotNull UUID loteId,
		@NotNull LocalDate fecha,
		@NotNull @DecimalMin("0.0") BigDecimal cantidad,
		@NotNull @DecimalMin("0.0") BigDecimal precioPorArroba,
		@NotBlank @Size(max = 160) String comprador) {
}
