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

/** Abonada: PROGRAMADA (plan, sin costo aun) o EJECUTADA (genera costo). RF04. */
@Entity
@Table(name = "fertilizaciones")
@Getter
@Setter
@NoArgsConstructor
public class Fertilizacion {

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
	private EstadoFertilizacion estado;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(nullable = false, length = 120)
	private String composicion;

	@Column(name = "dosis_kg_por_arbol", precision = 8, scale = 3)
	private BigDecimal dosisKgPorArbol;

	@Column(name = "cantidad_total_kg", precision = 12, scale = 2)
	private BigDecimal cantidadTotalKg;

	@Column(name = "costo_insumos", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoInsumos;

	@Column(name = "costo_mano_obra", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoManoObra;

	@Column(name = "costo_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal costoTotal;

	@Column(length = 300)
	private String observaciones;

	@Column(name = "fecha_registro", nullable = false)
	private Instant fechaRegistro;

	public Fertilizacion(Lote lote) {
		this.id = UUID.randomUUID();
		this.lote = lote;
		this.loteId = lote == null ? null : lote.getId();
		this.fechaRegistro = Instant.now();
	}

	public void recalcularCostoTotal() {
		BigDecimal insumos = costoInsumos == null ? BigDecimal.ZERO : costoInsumos;
		BigDecimal manoObra = costoManoObra == null ? BigDecimal.ZERO : costoManoObra;
		this.costoInsumos = insumos;
		this.costoManoObra = manoObra;
		this.costoTotal = insumos.add(manoObra);
	}
}
