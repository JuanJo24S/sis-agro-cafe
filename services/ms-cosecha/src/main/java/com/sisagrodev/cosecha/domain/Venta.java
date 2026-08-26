package com.sisagrodev.cosecha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Venta de cafe procesado; origen del evento IngresoRegistrado (RF06). */
@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
public class Venta {

	@Id
	private UUID id;

	@Column(name = "lote_id", nullable = false)
	private UUID loteId;

	@Column(name = "finca_id", nullable = false)
	private UUID fincaId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal cantidad;

	@Column(name = "precio_por_arroba", nullable = false, precision = 14, scale = 2)
	private BigDecimal precioPorArroba;

	@Column(nullable = false, length = 160)
	private String comprador;

	@Column(name = "monto_total", nullable = false, precision = 16, scale = 2)
	private BigDecimal montoTotal;

	@Column(name = "fecha_registro", nullable = false)
	private Instant fechaRegistro;

	@Column(nullable = false)
	private boolean anulada;

	public Venta(UUID loteId, UUID fincaId, UUID usuarioId) {
		this.id = UUID.randomUUID();
		this.loteId = loteId;
		this.fincaId = fincaId;
		this.usuarioId = usuarioId;
		this.fechaRegistro = Instant.now();
		this.anulada = false;
	}

	/** montoTotal = cantidad * precioPorArroba. Es el monto publicado en Kafka. */
	public void recalcularMontoTotal() {
		this.montoTotal = cantidad.multiply(precioPorArroba);
	}
}
