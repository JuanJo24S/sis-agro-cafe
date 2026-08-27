package com.sisagrodev.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class ManejadorErroresGraphQLTest {

	private ManejadorErroresGraphQL manejador;
	private DataFetchingEnvironment entorno;

	@BeforeEach
	void setUp() {
		manejador = new ManejadorErroresGraphQL();
		entorno = mock(DataFetchingEnvironment.class);
		ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
		when(stepInfo.getPath()).thenReturn(ResultPath.parse("/testField"));
		when(entorno.getExecutionStepInfo()).thenReturn(stepInfo);
		Field field = mock(Field.class);
		when(field.getSourceLocation()).thenReturn(new SourceLocation(1, 1));
		when(entorno.getField()).thenReturn(field);
	}

	@ParameterizedTest(name = "status {0} se clasifica como {1}")
	@CsvSource({
			"400, BAD_REQUEST",
			"422, BAD_REQUEST",
			"401, UNAUTHORIZED",
			"403, FORBIDDEN",
			"404, NOT_FOUND",
			"500, INTERNAL_ERROR"
	})
	@DisplayName("clasifica estados HTTP a ErrorType de GraphQL correctamente")
	void clasificarEstadosHttp(int estado, String tipoEsperado) {
		RestClientResponseException excepcion = estado >= 500
				? HttpServerErrorException.create(HttpStatusCode.valueOf(estado), "Error de servidor",
						HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)
				: HttpClientErrorException.create(HttpStatusCode.valueOf(estado), "Error de cliente",
						HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

		List<GraphQLError> errores = manejador.resolveToMultipleErrors(excepcion, entorno);

		assertThat(errores).isNotNull().hasSize(1);
		assertThat(errores.get(0).getErrorType().toString()).isEqualTo(tipoEsperado);
	}

	@Test
	@DisplayName("ResourceAccessException se traduce a INTERNAL_ERROR con mensaje descriptivo")
	void resourceAccessExceptionSeTraduceAInternalError() {
		ResourceAccessException excepcion = new ResourceAccessException("Servicio caido");

		List<GraphQLError> errores = manejador.resolveToMultipleErrors(excepcion, entorno);

		assertThat(errores).isNotNull().hasSize(1);
		assertThat(errores.get(0).getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
		assertThat(errores.get(0).getMessage()).contains("Un microservicio no esta disponible");
	}

	@Test
	@DisplayName("excepcion no controlada retorna null para delegar al resolvedor por defecto")
	void excepcionNoControladaRetornaNull() {
		RuntimeException excepcion = new RuntimeException("Cualquier error");

		List<GraphQLError> errores = manejador.resolveToMultipleErrors(excepcion, entorno);

		assertThat(errores).isNull();
	}
}
