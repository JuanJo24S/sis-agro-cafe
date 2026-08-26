package com.sisagrodev.financiero.evento;

import com.sisagrodev.financiero.service.BalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * RNF01: la consolidacion financiera ocurre en segundo plano. El usuario que registro
 * la labor o la venta nunca espera por este procesamiento.
 */
@Component
public class ConsumidorMovimientos {

	private static final Logger log = LoggerFactory.getLogger(ConsumidorMovimientos.class);

	private final BalanceService balanceService;

	public ConsumidorMovimientos(BalanceService balanceService) {
		this.balanceService = balanceService;
	}

	@KafkaListener(
			topics = {"${agrocafe.kafka.topico-costos}", "${agrocafe.kafka.topico-ingresos}"},
			groupId = "${spring.kafka.consumer.group-id}")
	public void consumir(@Payload MovimientoFinancieroEvent evento,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topico) {
		log.debug("Recibido {} desde {}", evento.eventoId(), topico);
		balanceService.aplicar(evento, topico);
	}
}
