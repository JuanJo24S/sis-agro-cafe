package com.sisagrodev.agronomico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Labor cultural ejecutada (limpia, fumigacion, deshierbe...). RF03. */
@Entity
@Table(name = "labores")
@Getter
@Setter
@NoArgsConstructor
public class Labor {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lote_id", nullable = false)
	private Lote lote;

	/** Misma columna que la asociacion, en modo solo lectura (ver Lote#fincaId). */
	@Column(name = "lote_id", insertable = false, updatable = false)
	private UUID loteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoLabor tipo;

	@Column(length = 300)
	private String descripcion;

	@Column(name = "fecha_ejecucion", nullable = false)
	private LocalDate fechaEjecucion;

	@Column(precision = 8, scale = 2)
	private BigDecimal jornales;

	@Column(name = "costo_mano_obra", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoManoObra;

	@Column(name = "costo_insumos", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoInsumos;

	@Column(name = "costo_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoTotal;

	@Column(name = "fecha_registro", nullable = false)
	private Instant fechaRegistro;

	/** Anulacion logica: el historico se conserva y se compensa el balance. */
	@Column(nullable = false)
	private boolean anulada;

	public Labor(Lote lote) {
		this.id = UUID.randomUUID();
		this.lote = lote;
		this.loteId = lote == null ? null : lote.getId();
		this.fechaRegistro = Instant.now();
		this.anulada = false;
	}

	/** costoTotal = costoManoObra + costoInsumos. Es el monto publicado en Kafka. */
	public void recalcularCostoTotal() {
		BigDecimal manoObra = costoManoObra == null ? BigDecimal.ZERO : costoManoObra;
		BigDecimal insumos = costoInsumos == null ? BigDecimal.ZERO : costoInsumos;
		this.costoManoObra = manoObra;
		this.costoInsumos = insumos;
		this.costoTotal = manoObra.add(insumos);
	}
}
