package com.sisagrodev.agronomico.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Envia a Kafka el evento `CostoRegistrado` una vez confirmada la transaccion (RNF01). */
@Component
public class EnvioKafka {

	private static final Logger log = LoggerFactory.getLogger(EnvioKafka.class);

	private final KafkaTemplate<String, MovimientoFinancieroEvent> plantilla;
	private final String topicoCostos;
	private final String topicoCostosAnulados;

	public EnvioKafka(KafkaTemplate<String, MovimientoFinancieroEvent> plantilla,
			@Value("${agrocafe.kafka.topico-costos}") String topicoCostos,
			@Value("${agrocafe.kafka.topico-costos-anulados}") String topicoCostosAnulados) {
		this.plantilla = plantilla;
		this.topicoCostos = topicoCostos;
		this.topicoCostosAnulados = topicoCostosAnulados;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void enviar(MovimientoFinancieroEvent evento) {
		String topico = evento.operacion() == Operacion.ANULACION ? topicoCostosAnulados : topicoCostos;
		plantilla.send(topico, evento.loteId().toString(), evento)
				.whenComplete((resultado, error) -> {
					if (error != null) {
						log.error("No se pudo publicar {} en {}", evento.eventoId(), topico, error);
					} else {
						log.info("Publicado {} {} de {} por {} en lote {} en topico {}", evento.tipo(),
								evento.operacion(), evento.origen(), evento.monto(), evento.loteId(), topico);
					}
				});
	}
}
