package com.sisagrodev.cosecha.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Envia el evento a Kafka despues del commit; el topico depende del tipo (RNF01). */
@Component
public class EnvioKafka {

	private static final Logger log = LoggerFactory.getLogger(EnvioKafka.class);

	private final KafkaTemplate<String, MovimientoFinancieroEvent> plantilla;
	private final String topicoIngresos;
	private final String topicoCostos;

	public EnvioKafka(KafkaTemplate<String, MovimientoFinancieroEvent> plantilla,
			@Value("${agrocafe.kafka.topico-ingresos}") String topicoIngresos,
			@Value("${agrocafe.kafka.topico-costos}") String topicoCostos) {
		this.plantilla = plantilla;
		this.topicoIngresos = topicoIngresos;
		this.topicoCostos = topicoCostos;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void enviar(MovimientoFinancieroEvent evento) {
		String topico = evento.tipo() == TipoMovimiento.INGRESO ? topicoIngresos : topicoCostos;
		plantilla.send(topico, evento.loteId().toString(), evento)
				.whenComplete((resultado, error) -> {
					if (error != null) {
						log.error("No se pudo publicar {} en {}", evento.eventoId(), topico, error);
					} else {
						log.info("Publicado {} {} de {} por {} en lote {}", evento.tipo(),
								evento.operacion(), evento.origen(), evento.monto(), evento.loteId());
					}
				});
	}
}
