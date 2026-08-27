package com.sisagrodev.gateway.cliente;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Utilidades HTTP del BFF: propagan el JWT del cliente hacia cada microservicio. */
public final class Peticiones {

	private Peticiones() {
	}

	public static <T> T obtener(RestClient cliente, String autorizacion, Class<T> tipo, String uri,
			Object... variables) {
		RestClient.RequestHeadersSpec<?> peticion = cliente.get().uri(uri, variables);
		autorizar(peticion, autorizacion);
		return peticion.retrieve().body(tipo);
	}

	public static <T> T obtener(RestClient cliente, String autorizacion,
			ParameterizedTypeReference<T> tipo, String uri, Object... variables) {
		RestClient.RequestHeadersSpec<?> peticion = cliente.get().uri(uri, variables);
		autorizar(peticion, autorizacion);
		return peticion.retrieve().body(tipo);
	}

	/** Igual que {@link #obtener}, pero un 404 significa "todavia no hay datos", no un error. */
	public static <T> T obtenerOpcional(RestClient cliente, String autorizacion, Class<T> tipo,
			String uri, Object... variables) {
		try {
			return obtener(cliente, autorizacion, tipo, uri, variables);
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
	}

	public static <T> T publicar(RestClient cliente, String autorizacion, Object cuerpo,
			Class<T> tipo, String uri, Object... variables) {
		return publicar(cliente, autorizacion, cuerpo, null, tipo, uri, variables);
	}

	public static void publicarVacio(RestClient cliente, String autorizacion, Object cuerpo,
			String uri, Object... variables) {
		RestClient.RequestBodySpec peticion = cliente.post().uri(uri, variables);
		autorizar(peticion, autorizacion);
		peticion.body(cuerpo == null ? Map.of() : cuerpo).retrieve().toBodilessEntity();
	}

	public static <T> T publicar(RestClient cliente, String autorizacion, Object cuerpo,
			String claveIdempotencia, Class<T> tipo, String uri, Object... variables) {
		RestClient.RequestBodySpec peticion = cliente.post().uri(uri, variables);
		autorizar(peticion, autorizacion);
		if (claveIdempotencia != null && !claveIdempotencia.isBlank()) {
			peticion.header("Idempotency-Key", claveIdempotencia);
		}
		return peticion.body(cuerpo == null ? Map.of() : cuerpo).retrieve().body(tipo);
	}

	public static <T> T parchear(RestClient cliente, String autorizacion, Object cuerpo,
			Class<T> tipo, String uri, Object... variables) {
		RestClient.RequestBodySpec peticion = cliente.patch().uri(uri, variables);
		autorizar(peticion, autorizacion);
		return peticion.body(cuerpo).retrieve().body(tipo);
	}

	public static void eliminar(RestClient cliente, String autorizacion, String uri,
			Object... variables) {
		RestClient.RequestHeadersSpec<?> peticion = cliente.delete().uri(uri, variables);
		autorizar(peticion, autorizacion);
		peticion.retrieve().toBodilessEntity();
	}

	private static void autorizar(RestClient.RequestHeadersSpec<?> peticion, String autorizacion) {
		if (autorizacion != null && !autorizacion.isBlank()) {
			peticion.header(HttpHeaders.AUTHORIZATION, autorizacion);
		}
	}
}
