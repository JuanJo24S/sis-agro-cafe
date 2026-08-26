package com.sisagrodev.cosecha.evento;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publica el movimiento como evento de aplicacion. El envio real a Kafka ocurre
 * despues del commit (ver {@link EnvioKafka}) para no emitir eventos de datos que
 * al final no se persistieron.
 */
@Component
public class PublicadorMovimientos {

	private final ApplicationEventPublisher publicador;

	public PublicadorMovimientos(ApplicationEventPublisher publicador) {
		this.publicador = publicador;
	}

	public void publicar(MovimientoFinancieroEvent evento) {
		publicador.publishEvent(evento);
	}
}
