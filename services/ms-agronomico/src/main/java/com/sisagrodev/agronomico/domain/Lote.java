package com.sisagrodev.agronomico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Parcela de cultivo. El cafe ya cosechado ("partida") vive en ms-cosecha. */
@Entity
@Table(name = "lotes")
@Getter
@Setter
@NoArgsConstructor
public class Lote {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "finca_id", nullable = false)
	private Finca finca;

	/**
	 * Misma columna que la asociacion, en modo solo lectura. Permite armar la respuesta
	 * sin inicializar el proxy de la finca (las entidades salen del servicio ya fuera
	 * de la transaccion, con `open-in-view: false`).
	 */
	@Column(name = "finca_id", insertable = false, updatable = false)
	private UUID fincaId;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 80)
	private String variedad;

	@Column(name = "fecha_siembra", nullable = false)
	private LocalDate fechaSiembra;

	@Column(name = "area_hectareas", nullable = false, precision = 12, scale = 4)
	private BigDecimal areaHectareas;

	@Column(name = "densidad_siembra")
	private Integer densidadSiembra;

	@Column(name = "altura_msnm")
	private Integer alturaMsnm;

	@Column(name = "ph_suelo", precision = 4, scale = 2)
	private BigDecimal phSuelo;

	@Column(name = "tipo_suelo", length = 120)
	private String tipoSuelo;

	@Column(name = "temperatura_promedio", precision = 5, scale = 2)
	private BigDecimal temperaturaPromedio;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	public Lote(Finca finca) {
		this.id = UUID.randomUUID();
		this.finca = finca;
		this.fincaId = finca.getId();
		this.fechaCreacion = Instant.now();
	}
}
