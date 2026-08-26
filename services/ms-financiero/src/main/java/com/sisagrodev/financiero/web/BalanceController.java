package com.sisagrodev.financiero.web;

import com.sisagrodev.financiero.seguridad.UsuarioAutenticado;
import com.sisagrodev.financiero.service.BalanceService;
import com.sisagrodev.financiero.web.dto.BalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/balance")
@Tag(name = "Balance")
public class BalanceController {

	private final BalanceService servicio;

	public BalanceController(BalanceService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/lotes/{loteId}")
	@Operation(summary = "Balance financiero consolidado de un lote (RF07)")
	public BalanceResponse porLote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID loteId) {
		return BalanceResponse.de(servicio.porLote(loteId, UsuarioAutenticado.id(jwt)));
	}

	@GetMapping("/lotes")
	@Operation(summary = "Balance de varios lotes por id (batch para el Gateway GraphQL)")
	public List<BalanceResponse> porLotes(@AuthenticationPrincipal Jwt jwt,
			@RequestParam List<UUID> ids) {
		return servicio.porLotes(ids, UsuarioAutenticado.id(jwt)).stream()
				.map(BalanceResponse::de)
				.toList();
	}

	@GetMapping("/fincas/{fincaId}")
	@Operation(summary = "Balance financiero consolidado de una finca (RF07)")
	public BalanceResponse porFinca(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fincaId) {
		return BalanceResponse.deFinca(fincaId,
				servicio.porFinca(fincaId, UsuarioAutenticado.id(jwt)));
	}
}
