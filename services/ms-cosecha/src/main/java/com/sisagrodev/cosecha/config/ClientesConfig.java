package com.sisagrodev.cosecha.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientesConfig {

	@Bean
	public RestClient restAgronomico(RestClient.Builder constructor,
			@Value("${agrocafe.clientes.agronomico.url}") String urlBase) {
		return constructor.baseUrl(urlBase).build();
	}
}
