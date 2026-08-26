package com.sisagrodev.gateway.web;

import com.sisagrodev.gateway.cliente.Peticiones;
import com.sisagrodev.gateway.config.PropagacionToken;
import com.sisagrodev.gateway.dto.Dtos;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

@Controller
public class AuthGraphQlController {

	private final RestClient clienteAuth;

	public AuthGraphQlController(RestClient clienteAuth) {
		this.clienteAuth = clienteAuth;
	}

	@QueryMapping
	public Dtos.Usuario me(
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(clienteAuth, autorizacion, Dtos.Usuario.class,
				"/api/v1/usuarios/me");
	}

	@MutationMapping
	public Dtos.Usuario registro(@Argument Map<String, Object> input) {
		return Peticiones.publicar(clienteAuth, null, input, Dtos.Usuario.class,
				"/api/v1/auth/registro");
	}

	@MutationMapping
	public Dtos.Token login(@Argument Map<String, Object> input) {
		return Peticiones.publicar(clienteAuth, null, input, Dtos.Token.class, "/api/v1/auth/login");
	}
}
