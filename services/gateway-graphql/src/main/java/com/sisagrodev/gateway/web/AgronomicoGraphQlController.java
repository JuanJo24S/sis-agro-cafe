package com.sisagrodev.gateway.web;

import com.sisagrodev.gateway.cliente.Peticiones;
import com.sisagrodev.gateway.config.PropagacionToken;
import com.sisagrodev.gateway.dto.Dtos;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

/** Consultas y mutaciones que resuelve ms-agronomico. */
@Controller
public class AgronomicoGraphQlController {

	private static final ParameterizedTypeReference<Dtos.Pagina<Dtos.Finca>> PAGINA_FINCAS =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<Dtos.Pagina<Dtos.Lote>> PAGINA_LOTES =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<Dtos.Pagina<Dtos.Labor>> PAGINA_LABORES =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<Dtos.Pagina<Dtos.Fertilizacion>> PAGINA_FERTILIZACIONES =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<List<Dtos.Lote>> LISTA_LOTES =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient cliente;

	public AgronomicoGraphQlController(RestClient clienteAgronomico) {
		this.cliente = clienteAgronomico;
	}

	// ----------------------------- Queries -----------------------------

	@QueryMapping
	public Dtos.Pagina<Dtos.Finca> fincas(@Argument int page, @Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, PAGINA_FINCAS,
				"/api/v1/fincas?page={page}&size={size}", page, size);
	}

	@QueryMapping
	public Dtos.Finca finca(@Argument String id,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, Dtos.Finca.class, "/api/v1/fincas/{id}", id);
	}

	@QueryMapping
	public Dtos.Lote lote(@Argument String id,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, Dtos.Lote.class, "/api/v1/lotes/{id}", id);
	}

	@QueryMapping
	public List<Dtos.Lote> lotes(@Argument List<String> ids,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, LISTA_LOTES, "/api/v1/lotes?ids={ids}",
				String.join(",", ids));
	}

	// ------------------------- Campos anidados -------------------------

	@SchemaMapping(typeName = "Finca", field = "lotes")
	public List<Dtos.Lote> lotesDeFinca(Dtos.Finca finca, @Argument int page, @Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, PAGINA_LOTES,
				"/api/v1/fincas/{id}/lotes?page={page}&size={size}", finca.id(), page, size)
				.content();
	}

	@SchemaMapping(typeName = "Lote", field = "labores")
	public List<Dtos.Labor> laboresDeLote(Dtos.Lote lote, @Argument int page, @Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, PAGINA_LABORES,
				"/api/v1/lotes/{id}/labores?page={page}&size={size}", lote.id(), page, size)
				.content();
	}

	@SchemaMapping(typeName = "Lote", field = "fertilizaciones")
	public List<Dtos.Fertilizacion> fertilizacionesDeLote(Dtos.Lote lote,
			@Argument String estado, @Argument int page, @Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		String uri = "/api/v1/lotes/{id}/fertilizaciones?page={page}&size={size}"
				+ (estado == null ? "" : "&estado=" + estado);
		return Peticiones.obtener(cliente, autorizacion, PAGINA_FERTILIZACIONES, uri, lote.id(),
				page, size).content();
	}

	// ---------------------------- Mutations ----------------------------

	@MutationMapping
	public Dtos.Finca crearFinca(@Argument Map<String, Object> input,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, Dtos.Finca.class, "/api/v1/fincas");
	}

	@MutationMapping
	public Dtos.Lote crearLote(@Argument String fincaId, @Argument Map<String, Object> input,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, Dtos.Lote.class,
				"/api/v1/fincas/{fincaId}/lotes", fincaId);
	}

	@MutationMapping
	public Dtos.Labor registrarLabor(@Argument String loteId, @Argument Map<String, Object> input,
			@Argument String idempotencyKey,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, idempotencyKey, Dtos.Labor.class,
				"/api/v1/lotes/{loteId}/labores", loteId);
	}

	@MutationMapping
	public boolean anularLabor(@Argument String laborId,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		Peticiones.eliminar(cliente, autorizacion, "/api/v1/labores/{laborId}", laborId);
		return true;
	}

	@MutationMapping
	public Dtos.Fertilizacion registrarFertilizacion(@Argument String loteId,
			@Argument Map<String, Object> input, @Argument String idempotencyKey,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, idempotencyKey,
				Dtos.Fertilizacion.class, "/api/v1/lotes/{loteId}/fertilizaciones", loteId);
	}

	@MutationMapping
	public Dtos.Fertilizacion ejecutarFertilizacion(@Argument String fertilizacionId,
			@Argument Map<String, Object> input,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, Dtos.Fertilizacion.class,
				"/api/v1/fertilizaciones/{id}/ejecucion", fertilizacionId);
	}
}
