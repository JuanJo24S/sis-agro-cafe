package com.sisagrodev.agronomico.web.dto;

import com.sisagrodev.agronomico.domain.Lote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoteResponse(
		UUID id,
		UUID fincaId,
		String nombre,
		String variedad,
		LocalDate fechaSiembra,
		BigDecimal areaHectareas,
		Integer densidadSiembra,
		Integer alturaMsnm,
		BigDecimal phSuelo,
		String tipoSuelo,
		BigDecimal temperaturaPromedio) {

	public static LoteResponse de(Lote lote) {
		return new LoteResponse(lote.getId(), lote.getFincaId(), lote.getNombre(),
				lote.getVariedad(), lote.getFechaSiembra(), lote.getAreaHectareas(),
				lote.getDensidadSiembra(), lote.getAlturaMsnm(), lote.getPhSuelo(),
				lote.getTipoSuelo(), lote.getTemperaturaPromedio());
	}
}
