package com.sisagrodev.financiero.web.dto;

import com.sisagrodev.financiero.domain.BalanceLote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record BalanceResponse(
		UUID loteId,
		UUID fincaId,
		BigDecimal costoTotal,
		BigDecimal ingresoTotal,
		BigDecimal balance,
		Instant actualizadoEn) {

	public static BalanceResponse de(BalanceLote balance) {
		return new BalanceResponse(balance.getLoteId(), balance.getFincaId(),
				balance.getCostoTotal(), balance.getIngresoTotal(), balance.balance(),
				balance.getActualizadoEn());
	}

	/** Agrega los balances de todos los lotes de una finca en un solo resultado. */
	public static BalanceResponse deFinca(UUID fincaId, List<BalanceLote> lotes) {
		BigDecimal costos = lotes.stream().map(BalanceLote::getCostoTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal ingresos = lotes.stream().map(BalanceLote::getIngresoTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		Instant ultima = lotes.stream().map(BalanceLote::getActualizadoEn)
				.max(Comparator.naturalOrder()).orElse(null);
		return new BalanceResponse(null, fincaId, costos, ingresos, ingresos.subtract(costos),
				ultima);
	}
}
