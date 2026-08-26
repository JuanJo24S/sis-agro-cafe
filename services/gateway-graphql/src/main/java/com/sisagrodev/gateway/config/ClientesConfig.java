package com.sisagrodev.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Un RestClient por microservicio de negocio. */
@Configuration
public class ClientesConfig {

	@Bean
	public RestClient clienteAuth(RestClient.Builder constructor,
			@Value("${agrocafe.clientes.auth.url}") String url) {
		return constructor.baseUrl(url).build();
	}

	@Bean
	public RestClient clienteAgronomico(RestClient.Builder constructor,
			@Value("${agrocafe.clientes.agronomico.url}") String url) {
		return constructor.baseUrl(url).build();
	}

	@Bean
	public RestClient clienteCosecha(RestClient.Builder constructor,
			@Value("${agrocafe.clientes.cosecha.url}") String url) {
		return constructor.baseUrl(url).build();
	}

	@Bean
	public RestClient clienteFinanciero(RestClient.Builder constructor,
			@Value("${agrocafe.clientes.financiero.url}") String url) {
		return constructor.baseUrl(url).build();
	}
}
