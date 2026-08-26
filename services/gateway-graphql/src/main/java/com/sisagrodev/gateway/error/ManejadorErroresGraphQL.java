package com.sisagrodev.gateway.error;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Traduce los errores HTTP de los microservicios a errores GraphQL con una
 * clasificacion util para el cliente (401 -> UNAUTHORIZED, 404 -> NOT_FOUND...).
 */
@Component
public class ManejadorErroresGraphQL extends DataFetcherExceptionResolverAdapter {

	@Override
	protected List<GraphQLError> resolveToMultipleErrors(Throwable excepcion,
			DataFetchingEnvironment entorno) {
		if (excepcion instanceof RestClientResponseException httpError) {
			return List.of(GraphQLError.newError()
					.errorType(clasificar(httpError.getStatusCode().value()))
					.message(mensaje(httpError))
					.path(entorno.getExecutionStepInfo().getPath())
					.location(entorno.getField().getSourceLocation())
					.build());
		}
		if (excepcion instanceof ResourceAccessException) {
			return List.of(GraphQLError.newError()
					.errorType(ErrorType.INTERNAL_ERROR)
					.message("Un microservicio no esta disponible en este momento.")
					.path(entorno.getExecutionStepInfo().getPath())
					.location(entorno.getField().getSourceLocation())
					.build());
		}
		return null;
	}

	private ErrorType clasificar(int estado) {
		return switch (estado) {
			case 400, 422 -> ErrorType.BAD_REQUEST;
			case 401 -> ErrorType.UNAUTHORIZED;
			case 403 -> ErrorType.FORBIDDEN;
			case 404 -> ErrorType.NOT_FOUND;
			default -> ErrorType.INTERNAL_ERROR;
		};
	}

	private String mensaje(RestClientResponseException error) {
		var problema = error.getResponseBodyAs(java.util.Map.class);
		if (problema != null && problema.get("detail") != null) {
			return String.valueOf(problema.get("detail"));
		}
		return error.getStatusText();
	}
}
