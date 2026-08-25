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

import com.sisagrodev.financiero.web.dto.PaginaResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/balance")
@Tag(name = "Balance")
@Validated
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

	@GetMapping("/fincas/{fincaId}/lotes")
	@Operation(summary = "Listar los balances de los lotes de una finca con paginacion (RF07)")
	public PaginaResponse<BalanceResponse> porFincaPaginado(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fincaId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
		return PaginaResponse.de(servicio.porFincaPaginado(fincaId, UsuarioAutenticado.id(jwt),
				Paginaciones.de(page, size, "actualizadoEn")), BalanceResponse::de);
	}

	@GetMapping
	@Operation(summary = "Listar balances de todos los lotes del usuario con paginacion (RF07)")
	public PaginaResponse<BalanceResponse> listarTodosPaginado(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
		return PaginaResponse.de(servicio.listarPaginado(UsuarioAutenticado.id(jwt),
				Paginaciones.de(page, size, "actualizadoEn")), BalanceResponse::de);
	}
}
