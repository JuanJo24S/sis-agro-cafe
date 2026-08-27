package com.sisagrodev.gateway.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Un RestClient por microservicio de negocio con timeouts explicitos. */
@Configuration
public class ClientesConfig {

	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory(
			@Value("${agrocafe.clientes.timeout.conexion:3s}") Duration connectTimeout,
			@Value("${agrocafe.clientes.timeout.lectura:10s}") Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		return factory;
	}

	@Bean
	public RestClient clienteAuth(RestClient.Builder constructor,
			ClientHttpRequestFactory factory,
			@Value("${agrocafe.clientes.auth.url}") String url) {
		return constructor.requestFactory(factory).baseUrl(url).build();
	}

	@Bean
	public RestClient clienteAgronomico(RestClient.Builder constructor,
			ClientHttpRequestFactory factory,
			@Value("${agrocafe.clientes.agronomico.url}") String url) {
		return constructor.requestFactory(factory).baseUrl(url).build();
	}

	@Bean
	public RestClient clienteCosecha(RestClient.Builder constructor,
			ClientHttpRequestFactory factory,
			@Value("${agrocafe.clientes.cosecha.url}") String url) {
		return constructor.requestFactory(factory).baseUrl(url).build();
	}

	@Bean
	public RestClient clienteFinanciero(RestClient.Builder constructor,
			ClientHttpRequestFactory factory,
			@Value("${agrocafe.clientes.financiero.url}") String url) {
		return constructor.requestFactory(factory).baseUrl(url).build();
	}
}
