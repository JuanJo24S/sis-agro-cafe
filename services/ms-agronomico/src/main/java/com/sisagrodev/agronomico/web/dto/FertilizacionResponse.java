package com.sisagrodev.agronomico.web.dto;

import com.sisagrodev.agronomico.domain.EstadoFertilizacion;
import com.sisagrodev.agronomico.domain.Fertilizacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FertilizacionResponse(
		UUID id,
		UUID loteId,
		EstadoFertilizacion estado,
		LocalDate fecha,
		String composicion,
		BigDecimal dosisKgPorArbol,
		BigDecimal cantidadTotalKg,
		BigDecimal costoInsumos,
		BigDecimal costoManoObra,
		BigDecimal costoTotal,
		String observaciones,
		Instant fechaRegistro) {

	public static FertilizacionResponse de(Fertilizacion fertilizacion) {
		return new FertilizacionResponse(fertilizacion.getId(), fertilizacion.getLoteId(),
				fertilizacion.getEstado(), fertilizacion.getFecha(), fertilizacion.getComposicion(),
				fertilizacion.getDosisKgPorArbol(), fertilizacion.getCantidadTotalKg(),
				fertilizacion.getCostoInsumos(), fertilizacion.getCostoManoObra(),
				fertilizacion.getCostoTotal(), fertilizacion.getObservaciones(),
				fertilizacion.getFechaRegistro());
	}
}
