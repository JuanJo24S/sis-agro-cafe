package com.sisagrodev.agronomico.evento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contrato de evento compartido con ms-cosecha (productor) y ms-financiero (consumidor).
 * Viaja por el topico `costo-registrado` cuando `tipo = COSTO` y por `ingreso-registrado`
 * cuando `tipo = INGRESO`. `eventoId` permite consumo idempotente en ms-financiero.
 */
public record MovimientoFinancieroEvent(
		UUID eventoId,
		TipoMovimiento tipo,
		Operacion operacion,
		OrigenMovimiento origen,
		UUID origenId,
		UUID loteId,
		UUID fincaId,
		UUID usuarioId,
		BigDecimal monto,
		LocalDate fecha,
		Instant ocurridoEn) {

	public static MovimientoFinancieroEvent costo(Operacion operacion, OrigenMovimiento origen,
			UUID origenId, UUID loteId, UUID fincaId, UUID usuarioId, BigDecimal monto,
			LocalDate fecha) {
		return new MovimientoFinancieroEvent(UUID.randomUUID(), TipoMovimiento.COSTO, operacion,
				origen, origenId, loteId, fincaId, usuarioId, monto, fecha, Instant.now());
	}
}
