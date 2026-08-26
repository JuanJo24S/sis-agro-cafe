package com.sisagrodev.financiero.evento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contrato de evento consumido desde los topicos `costo-registrado` (ms-agronomico,
 * ms-cosecha) e `ingreso-registrado` (ms-cosecha). Debe mantenerse alineado con la
 * definicion de los productores.
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
}
