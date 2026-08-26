package com.sisagrodev.financiero.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declara ambos topicos para que el consumidor arranque aunque nadie haya publicado aun. */
@Configuration
public class KafkaConfig {

	@Bean
	public NewTopic topicoCostos(@Value("${agrocafe.kafka.topico-costos}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoIngresos(@Value("${agrocafe.kafka.topico-ingresos}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}
}
