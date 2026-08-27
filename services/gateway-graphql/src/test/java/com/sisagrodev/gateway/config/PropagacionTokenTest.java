package com.sisagrodev.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionInput;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

class PropagacionTokenTest {

	private PropagacionToken interceptor;

	@BeforeEach
	void setUp() {
		interceptor = new PropagacionToken();
	}

	@Test
	@DisplayName("extrae cabecera Authorization y la guarda en GraphQLContext")
	void extraeCabeceraAuthorization() {
		WebGraphQlRequest request = mock(WebGraphQlRequest.class);
		WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
		WebGraphQlResponse response = mock(WebGraphQlResponse.class);

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer test-jwt-token");
		when(request.getHeaders()).thenReturn(headers);
		when(chain.next(request)).thenReturn(Mono.just(response));

		AtomicReference<ExecutionInput> inputCapturado = new AtomicReference<>();
		doAnswer(invocation -> {
			BiFunction<ExecutionInput, ExecutionInput.Builder, ExecutionInput> configurador =
					invocation.getArgument(0);
			ExecutionInput original = ExecutionInput.newExecutionInput().query("{ me { id } }").build();
			ExecutionInput modificado = configurador.apply(original, ExecutionInput.newExecutionInput().query("{ me { id } }"));
			inputCapturado.set(modificado);
			return null;
		}).when(request).configureExecutionInput(any());

		WebGraphQlResponse resultado = interceptor.intercept(request, chain).block();

		assertThat(resultado).isSameAs(response);
		verify(chain).next(request);
		assertThat(inputCapturado.get()).isNotNull();
		assertThat(inputCapturado.get().getGraphQLContext().<String>get(PropagacionToken.CLAVE_AUTORIZACION))
				.isEqualTo("Bearer test-jwt-token");
	}

	@Test
	@DisplayName("si cabecera Authorization no esta presente guarda cadena vacia")
	void cabeceraAusenteGuardaCadenaVacia() {
		WebGraphQlRequest request = mock(WebGraphQlRequest.class);
		WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
		WebGraphQlResponse response = mock(WebGraphQlResponse.class);

		when(request.getHeaders()).thenReturn(new HttpHeaders());
		when(chain.next(request)).thenReturn(Mono.just(response));

		AtomicReference<ExecutionInput> inputCapturado = new AtomicReference<>();
		doAnswer(invocation -> {
			BiFunction<ExecutionInput, ExecutionInput.Builder, ExecutionInput> configurador =
					invocation.getArgument(0);
			ExecutionInput original = ExecutionInput.newExecutionInput().query("{ me { id } }").build();
			ExecutionInput modificado = configurador.apply(original, ExecutionInput.newExecutionInput().query("{ me { id } }"));
			inputCapturado.set(modificado);
			return null;
		}).when(request).configureExecutionInput(any());

		interceptor.intercept(request, chain).block();

		assertThat(inputCapturado.get().getGraphQLContext().<String>get(PropagacionToken.CLAVE_AUTORIZACION))
				.isEmpty();
	}
}
