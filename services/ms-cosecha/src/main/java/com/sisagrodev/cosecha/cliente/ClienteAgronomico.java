package com.sisagrodev.cosecha.cliente;

import com.sisagrodev.cosecha.error.ExcepcionNegocio;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

/**
 * Unica dependencia sincrona de ms-cosecha: valida que el lote exista, que sea del
 * usuario autenticado y obtiene su `fincaId` (necesario para el balance por finca).
 * Se propaga el mismo JWT del cliente, no hay credencial de servicio.
 */
@Component
public class ClienteAgronomico {

	private final RestClient cliente;

	public ClienteAgronomico(RestClient restAgronomico) {
		this.cliente = restAgronomico;
	}

	public LoteRef obtenerLote(UUID loteId, String token) {
		try {
			return cliente.get()
					.uri("/api/v1/lotes/{loteId}", loteId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.retrieve()
					.onStatus(estado -> estado.value() == HttpStatus.NOT_FOUND.value(),
							(peticion, respuesta) -> {
								throw ExcepcionNegocio.noEncontrado(
										"El lote %s no existe en ms-agronomico.".formatted(loteId));
							})
					.onStatus(estado -> estado.value() == HttpStatus.FORBIDDEN.value(),
							(peticion, respuesta) -> {
								throw ExcepcionNegocio.prohibido(
										"El lote no pertenece al usuario autenticado.");
							})
					.body(LoteRef.class);
		}
		catch (ResourceAccessException ex) {
			throw new ExcepcionNegocio(HttpStatus.SERVICE_UNAVAILABLE, "Dependencia no disponible",
					"ms-agronomico no responde; intente de nuevo en unos segundos.");
		}
	}
}
