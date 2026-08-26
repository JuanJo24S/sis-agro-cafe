package com.sisagrodev.cosecha.web.dto;

import com.sisagrodev.cosecha.domain.EstadoGrano;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecoleccionRequest(
		@NotNull LocalDate fecha,
		@NotNull @DecimalMin("0.0") BigDecimal pesoCascaraKg,
		@NotNull @DecimalMin("0.0") BigDecimal valorPorKgArroba,
		@Size(max = 120) String recolector,
		EstadoGrano estadoGrano) {
}
