package com.sisagrodev.agronomico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Soporta la cabecera `Idempotency-Key`: si el cliente reintenta el POST, se devuelve
 * el recurso ya creado en vez de duplicar el costo (y con el, el balance financiero).
 */
@Entity
@Table(name = "claves_idempotencia")
@Getter
@Setter
@NoArgsConstructor
public class ClaveIdempotencia {

	@Id
	private UUID clave;

	@Column(nullable = false, length = 40)
	private String recurso;

	@Column(name = "recurso_id", nullable = false)
	private UUID recursoId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	public ClaveIdempotencia(UUID clave, String recurso, UUID recursoId, UUID usuarioId) {
		this.clave = clave;
		this.recurso = recurso;
		this.recursoId = recursoId;
		this.usuarioId = usuarioId;
		this.creadaEn = Instant.now();
	}
}
