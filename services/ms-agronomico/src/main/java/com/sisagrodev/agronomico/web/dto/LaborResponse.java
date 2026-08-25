package com.sisagrodev.agronomico.web.dto;

import com.sisagrodev.agronomico.domain.Labor;
import com.sisagrodev.agronomico.domain.TipoLabor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LaborResponse(
		UUID id,
		UUID loteId,
		TipoLabor tipo,
		String descripcion,
		LocalDate fechaEjecucion,
		BigDecimal jornales,
		BigDecimal costoManoObra,
		BigDecimal costoInsumos,
		BigDecimal costoTotal,
		Instant fechaRegistro) {

	public static LaborResponse de(Labor labor) {
		return new LaborResponse(labor.getId(), labor.getLoteId(), labor.getTipo(),
				labor.getDescripcion(), labor.getFechaEjecucion(), labor.getJornales(),
				labor.getCostoManoObra(), labor.getCostoInsumos(), labor.getCostoTotal(),
				labor.getFechaRegistro());
	}
}
