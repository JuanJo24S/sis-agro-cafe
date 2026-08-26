package com.sisagrodev.cosecha.evento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mismo contrato de evento que publica ms-agronomico y consume ms-financiero.
 * `tipo = INGRESO` viaja por el topico de ingresos; `tipo = COSTO` por el de costos.
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

	public static MovimientoFinancieroEvent de(TipoMovimiento tipo, Operacion operacion,
			OrigenMovimiento origen, UUID origenId, UUID loteId, UUID fincaId, UUID usuarioId,
			BigDecimal monto, LocalDate fecha) {
		return new MovimientoFinancieroEvent(UUID.randomUUID(), tipo, operacion, origen, origenId,
				loteId, fincaId, usuarioId, monto, fecha, Instant.now());
	}
}
