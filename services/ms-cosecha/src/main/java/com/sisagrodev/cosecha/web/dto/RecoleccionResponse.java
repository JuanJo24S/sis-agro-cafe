package com.sisagrodev.cosecha.web.dto;

import com.sisagrodev.cosecha.domain.EstadoGrano;
import com.sisagrodev.cosecha.domain.Recoleccion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecoleccionResponse(
		UUID id,
		UUID loteId,
		LocalDate fecha,
		BigDecimal pesoCascaraKg,
		BigDecimal valorPorKgArroba,
		String recolector,
		EstadoGrano estadoGrano,
		BigDecimal costoManoObra,
		Instant fechaRegistro) {

	public static RecoleccionResponse de(Recoleccion recoleccion) {
		return new RecoleccionResponse(recoleccion.getId(), recoleccion.getLoteId(),
				recoleccion.getFecha(), recoleccion.getPesoCascaraKg(),
				recoleccion.getValorPorKgArroba(), recoleccion.getRecolector(),
				recoleccion.getEstadoGrano(), recoleccion.getCostoManoObra(),
				recoleccion.getFechaRegistro());
	}
}
