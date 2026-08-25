package com.sisagrodev.agronomico.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Crea el topico de costos al arrancar, para no depender de scripts externos. */
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
}
