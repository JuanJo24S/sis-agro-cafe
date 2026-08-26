package com.sisagrodev.cosecha.web.dto;

import com.sisagrodev.cosecha.domain.Venta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VentaResponse(
		UUID id,
		UUID loteId,
		LocalDate fecha,
		BigDecimal cantidad,
		BigDecimal precioPorArroba,
		String comprador,
		BigDecimal montoTotal,
		Instant fechaRegistro) {

	public static VentaResponse de(Venta venta) {
		return new VentaResponse(venta.getId(), venta.getLoteId(), venta.getFecha(),
				venta.getCantidad(), venta.getPrecioPorArroba(), venta.getComprador(),
				venta.getMontoTotal(), venta.getFechaRegistro());
	}
}
