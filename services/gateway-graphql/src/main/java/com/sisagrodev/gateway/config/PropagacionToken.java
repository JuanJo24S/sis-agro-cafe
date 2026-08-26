package com.sisagrodev.gateway.config;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * El gateway no valida el JWT: lo toma de la cabecera HTTP y lo propaga a cada
 * microservicio, que si lo valida localmente (RNF02). Asi `login` y `registro`
 * siguen siendo operaciones publicas sin configuracion adicional.
 */
@Component
public class PropagacionToken implements WebGraphQlInterceptor {

	public static final String CLAVE_AUTORIZACION = "autorizacion";

	@Override
	public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
		String autorizacion = peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		peticion.configureExecutionInput((entrada, constructor) -> constructor
				.graphQLContext(contexto -> contexto.put(CLAVE_AUTORIZACION,
						autorizacion == null ? "" : autorizacion))
				.build());
		return cadena.next(peticion);
	}
}
