package com.sisagrodev.cosecha.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

	@Bean
	public NewTopic topicoIngresos(@Value("${agrocafe.kafka.topico-ingresos}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoIngresosAnulados(@Value("${agrocafe.kafka.topico-ingresos-anulados}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}

	@Bean
	public NewTopic topicoCostosAnulados(@Value("${agrocafe.kafka.topico-costos-anulados}") String nombre) {
		return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
	}
}
