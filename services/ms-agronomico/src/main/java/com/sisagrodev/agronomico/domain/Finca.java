package com.sisagrodev.agronomico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fincas")
@Getter
@Setter
@NoArgsConstructor
public class Finca {

	@Id
	private UUID id;

	/** Id del usuario dueno de la finca, tomado del JWT (no lo envia el cliente). */
	@Column(name = "propietario_id", nullable = false)
	private UUID propietarioId;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 120)
	private String municipio;

	@Column(length = 120)
	private String departamento;

	@Column(name = "area_total_hectareas", precision = 12, scale = 4)
	private BigDecimal areaTotalHectareas;

	@Column(name = "fecha_creacion", nullable = false)
	private Instant fechaCreacion;

	public Finca(UUID propietarioId) {
		this.id = UUID.randomUUID();
		this.propietarioId = propietarioId;
		this.fechaCreacion = Instant.now();
	}
}
