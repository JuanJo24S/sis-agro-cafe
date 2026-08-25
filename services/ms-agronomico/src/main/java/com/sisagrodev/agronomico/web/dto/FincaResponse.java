package com.sisagrodev.agronomico.web.dto;

import com.sisagrodev.agronomico.domain.Finca;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FincaResponse(
		UUID id,
		UUID propietarioId,
		String nombre,
		String municipio,
		String departamento,
		BigDecimal areaTotalHectareas,
		Instant fechaCreacion) {

	public static FincaResponse de(Finca finca) {
		return new FincaResponse(finca.getId(), finca.getPropietarioId(), finca.getNombre(),
				finca.getMunicipio(), finca.getDepartamento(), finca.getAreaTotalHectareas(),
				finca.getFechaCreacion());
	}
}
