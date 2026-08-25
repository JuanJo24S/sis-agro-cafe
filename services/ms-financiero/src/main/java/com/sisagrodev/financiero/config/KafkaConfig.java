package com.sisagrodev.financiero.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Declara los topicos principales, los topicos de anulacion y los topicos DLT.
 * Configura reintentos y envio a DLT ante errores irrecuperables.
 */
@Configuration
public class KafkaConfig {

	@Bean
	public NewTopic topicoCostos(@Value("${agrocafe.kafka.topico-costos}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoCostosAnulados(@Value("${agrocafe.kafka.topico-costos-anulados}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoIngresos(@Value("${agrocafe.kafka.topico-ingresos}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoIngresosAnulados(@Value("${agrocafe.kafka.topico-ingresos-anulados}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoCostosDlt(@Value("${agrocafe.kafka.topico-costos}") String nombre) {
		return TopicBuilder.name(nombre + ".DLT").partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoIngresosDlt(@Value("${agrocafe.kafka.topico-ingresos}") String nombre) {
		return TopicBuilder.name(nombre + ".DLT").partitions(3).replicas(1).build();
	}

	@Bean
	public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
		// 3 reintentos con intervalo de 1 segundo antes de enviar a DLT
		return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
	}
}
