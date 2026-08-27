package com.sisagrodev.gateway.web;

import com.sisagrodev.gateway.cliente.Peticiones;
import com.sisagrodev.gateway.config.PropagacionToken;
import com.sisagrodev.gateway.dto.Dtos;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

/**
 * Balance consolidado (ms-financiero). `Lote.balance` se resuelve por lotes con
 * `@BatchMapping` contra el endpoint batch, para no hacer una peticion por lote.
 */
@Controller
public class FinancieroGraphQlController {

	private static final ParameterizedTypeReference<List<Dtos.Balance>> LISTA_BALANCES =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient cliente;

	public FinancieroGraphQlController(RestClient clienteFinanciero) {
		this.cliente = clienteFinanciero;
	}

	@QueryMapping
	public Dtos.Balance balanceLote(@Argument String loteId,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtenerOpcional(cliente, autorizacion, Dtos.Balance.class,
				"/api/v1/balance/lotes/{loteId}", loteId);
	}

	@QueryMapping
	public Dtos.Balance balanceFinca(@Argument String fincaId,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtenerOpcional(cliente, autorizacion, Dtos.Balance.class,
				"/api/v1/balance/fincas/{fincaId}", fincaId);
	}

	@SchemaMapping(typeName = "Finca", field = "balance")
	public Dtos.Balance balanceDeFinca(Dtos.Finca finca,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtenerOpcional(cliente, autorizacion, Dtos.Balance.class,
				"/api/v1/balance/fincas/{fincaId}", finca.id());
	}

	@BatchMapping(typeName = "Lote", field = "balance")
	public Map<Dtos.Lote, Dtos.Balance> balanceDeLotes(List<Dtos.Lote> lotes,
			GraphQLContext contexto) {
		String autorizacion = contexto.getOrDefault(PropagacionToken.CLAVE_AUTORIZACION, "");
		String ids = lotes.stream().map(Dtos.Lote::id).collect(Collectors.joining(","));
		List<Dtos.Balance> balances = Peticiones.obtener(cliente, autorizacion, LISTA_BALANCES,
				"/api/v1/balance/lotes?ids={ids}", ids);

		Map<String, Dtos.Balance> porLote = balances == null ? Map.of()
				: balances.stream().collect(Collectors.toMap(Dtos.Balance::loteId,
						Function.identity(), (primero, segundo) -> primero));

		Map<Dtos.Lote, Dtos.Balance> resultado = new HashMap<>();
		for (Dtos.Lote lote : lotes) {
			resultado.put(lote, porLote.get(lote.id()));
		}
		return resultado;
	}
}
