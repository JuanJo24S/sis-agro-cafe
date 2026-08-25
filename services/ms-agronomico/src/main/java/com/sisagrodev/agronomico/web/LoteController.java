package com.sisagrodev.agronomico.web;

import com.sisagrodev.agronomico.seguridad.UsuarioAutenticado;
import com.sisagrodev.agronomico.service.LoteService;
import com.sisagrodev.agronomico.web.dto.LoteRequest;
import com.sisagrodev.agronomico.web.dto.LoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lotes")
@Tag(name = "Lotes")
public class LoteController {

	private final LoteService servicio;

	public LoteController(LoteService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Consultar varios lotes por id (batch para el Gateway GraphQL)")
	public List<LoteResponse> porIds(@AuthenticationPrincipal Jwt jwt,
			@RequestParam List<UUID> ids) {
		return servicio.porIds(ids, UsuarioAutenticado.id(jwt)).stream()
				.map(LoteResponse::de)
				.toList();
	}

	@GetMapping("/{loteId}")
	@Operation(summary = "Consultar un lote (RF02)")
	public LoteResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID loteId) {
		return LoteResponse.de(servicio.delUsuario(loteId, UsuarioAutenticado.id(jwt)));
	}

	@PutMapping("/{loteId}")
	@Operation(summary = "Actualizar la caracterizacion de un lote (RF02)")
	public LoteResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID loteId,
			@Valid @RequestBody LoteRequest solicitud) {
		return LoteResponse.de(servicio.actualizar(loteId, UsuarioAutenticado.id(jwt), solicitud));
	}
}
