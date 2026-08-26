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

/** Consultas y mutaciones que resuelve ms-cosecha. */
@Controller
public class CosechaGraphQlController {

	private static final ParameterizedTypeReference<Dtos.Pagina<Dtos.Recoleccion>> PAGINA_RECOLECCIONES =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<Dtos.Pagina<Dtos.Venta>> PAGINA_VENTAS =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient cliente;

	public CosechaGraphQlController(RestClient clienteCosecha) {
		this.cliente = clienteCosecha;
	}

	@QueryMapping
	public Dtos.Pagina<Dtos.Venta> ventas(@Argument String loteId, @Argument int page,
			@Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		String uri = "/api/v1/ventas?page={page}&size={size}"
				+ (loteId == null ? "" : "&loteId=" + loteId);
		return Peticiones.obtener(cliente, autorizacion, PAGINA_VENTAS, uri, page, size);
	}

	@SchemaMapping(typeName = "Lote", field = "recolecciones")
	public List<Dtos.Recoleccion> recoleccionesDeLote(Dtos.Lote lote, @Argument int page,
			@Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, PAGINA_RECOLECCIONES,
				"/api/v1/lotes/{id}/recolecciones?page={page}&size={size}", lote.id(), page, size)
				.content();
	}

	@SchemaMapping(typeName = "Lote", field = "ventas")
	public List<Dtos.Venta> ventasDeLote(Dtos.Lote lote, @Argument int page, @Argument int size,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.obtener(cliente, autorizacion, PAGINA_VENTAS,
				"/api/v1/ventas?loteId={id}&page={page}&size={size}", lote.id(), page, size)
				.content();
	}

	@MutationMapping
	public Dtos.Recoleccion registrarRecoleccion(@Argument String loteId,
			@Argument Map<String, Object> input, @Argument String idempotencyKey,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, idempotencyKey,
				Dtos.Recoleccion.class, "/api/v1/lotes/{loteId}/recolecciones", loteId);
	}

	@MutationMapping
	public Dtos.Recoleccion actualizarEstadoGrano(@Argument String recoleccionId,
			@Argument String estadoGrano,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.parchear(cliente, autorizacion, Map.of("estadoGrano", estadoGrano),
				Dtos.Recoleccion.class, "/api/v1/recolecciones/{id}/estado", recoleccionId);
	}

	@MutationMapping
	public Dtos.Venta registrarVenta(@Argument Map<String, Object> input,
			@Argument String idempotencyKey,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		return Peticiones.publicar(cliente, autorizacion, input, idempotencyKey, Dtos.Venta.class,
				"/api/v1/ventas");
	}

	@MutationMapping
	public boolean anularVenta(@Argument String ventaId,
			@ContextValue(name = PropagacionToken.CLAVE_AUTORIZACION, required = false) String autorizacion) {
		Peticiones.eliminar(cliente, autorizacion, "/api/v1/ventas/{ventaId}", ventaId);
		return true;
	}
}
