package com.sisagrodev.financiero.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Proyeccion derivada de eventos: ms-financiero no es dueno de ningun dato propio,
 * solo acumula lo que publican ms-agronomico y ms-cosecha (RF07).
 */
@Entity
@Table(name = "balances_lote")
@Getter
@Setter
@NoArgsConstructor
public class BalanceLote {

	@Id
	@Column(name = "lote_id")
	private UUID loteId;

	@Column(name = "finca_id", nullable = false)
	private UUID fincaId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "costo_total", nullable = false, precision = 18, scale = 2)
	private BigDecimal costoTotal;

	@Column(name = "ingreso_total", nullable = false, precision = 18, scale = 2)
	private BigDecimal ingresoTotal;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@Version
	private long version;

	public BalanceLote(UUID loteId, UUID fincaId, UUID usuarioId) {
		this.loteId = loteId;
		this.fincaId = fincaId;
		this.usuarioId = usuarioId;
		this.costoTotal = BigDecimal.ZERO;
		this.ingresoTotal = BigDecimal.ZERO;
		this.actualizadoEn = Instant.now();
	}

	public void sumarCosto(BigDecimal monto) {
		this.costoTotal = this.costoTotal.add(monto);
		this.actualizadoEn = Instant.now();
	}

	public void sumarIngreso(BigDecimal monto) {
		this.ingresoTotal = this.ingresoTotal.add(monto);
		this.actualizadoEn = Instant.now();
	}

	/** balance = ingresoTotal - costoTotal. */
	public BigDecimal balance() {
		return ingresoTotal.subtract(costoTotal);
	}
}
