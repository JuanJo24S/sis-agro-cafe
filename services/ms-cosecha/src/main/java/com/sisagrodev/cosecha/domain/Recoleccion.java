package com.sisagrodev.cosecha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Jornada de recoleccion: peso en cascara y pago a recolectores (RF05). */
@Entity
@Table(name = "recolecciones")
@Getter
@Setter
@NoArgsConstructor
public class Recoleccion {

	@Id
	private UUID id;

	/** Lote (parcela) de ms-agronomico del que se recolecto. */
	@Column(name = "lote_id", nullable = false)
	private UUID loteId;

	@Column(name = "finca_id", nullable = false)
	private UUID fincaId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(name = "peso_cascara_kg", nullable = false, precision = 12, scale = 2)
	private BigDecimal pesoCascaraKg;

	@Column(name = "valor_por_kg_arroba", nullable = false, precision = 14, scale = 2)
	private BigDecimal valorPorKgArroba;

	@Column(length = 120)
	private String recolector;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_grano", nullable = false, length = 20)
	private EstadoGrano estadoGrano;

	@Column(name = "costo_mano_obra", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoManoObra;

	@Column(name = "fecha_registro", nullable = false)
	private Instant fechaRegistro;

	@Column(nullable = false)
	private boolean anulada;

	public Recoleccion(UUID loteId, UUID fincaId, UUID usuarioId) {
		this.id = UUID.randomUUID();
		this.loteId = loteId;
		this.fincaId = fincaId;
		this.usuarioId = usuarioId;
		this.estadoGrano = EstadoGrano.CEREZA;
		this.fechaRegistro = Instant.now();
		this.anulada = false;
	}

	/** costoManoObra = pesoCascaraKg * valorPorKgArroba. */
	public void recalcularCostoManoObra() {
		this.costoManoObra = pesoCascaraKg.multiply(valorPorKgArroba);
	}
}
