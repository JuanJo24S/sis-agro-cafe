package com.sisagrodev.financiero.domain;

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
 * Consumo idempotente: Kafka garantiza "al menos una vez", asi que un reintento no
 * debe volver a sumar el mismo movimiento al balance.
 */
@Entity
@Table(name = "eventos_procesados")
@Getter
@Setter
@NoArgsConstructor
public class EventoProcesado {

	@Id
	@Column(name = "evento_id")
	private UUID eventoId;

	@Column(nullable = false, length = 60)
	private String topico;

	@Column(name = "procesado_en", nullable = false)
	private Instant procesadoEn;

	public EventoProcesado(UUID eventoId, String topico) {
		this.eventoId = eventoId;
		this.topico = topico;
		this.procesadoEn = Instant.now();
	}
}
