package com.sisagrodev.agronomico.web;

import com.sisagrodev.agronomico.domain.Finca;
import com.sisagrodev.agronomico.seguridad.UsuarioAutenticado;
import com.sisagrodev.agronomico.service.FincaService;
import com.sisagrodev.agronomico.service.LoteService;
import com.sisagrodev.agronomico.web.dto.FincaRequest;
import com.sisagrodev.agronomico.web.dto.FincaResponse;
import com.sisagrodev.agronomico.web.dto.LoteRequest;
import com.sisagrodev.agronomico.web.dto.LoteResponse;
import com.sisagrodev.agronomico.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fincas")
@Tag(name = "Fincas")
@Validated
public class FincaController {

	private final FincaService fincaService;
	private final LoteService loteService;

	public FincaController(FincaService fincaService, LoteService loteService) {
		this.fincaService = fincaService;
		this.loteService = loteService;
	}

	@PostMapping
	@Operation(summary = "Registrar una finca (RF02)")
	public ResponseEntity<FincaResponse> crear(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody FincaRequest solicitud) {
		Finca finca = fincaService.crear(UsuarioAutenticado.id(jwt), solicitud);
		return ResponseEntity.created(URI.create("/api/v1/fincas/" + finca.getId()))
				.body(FincaResponse.de(finca));
	}

	@GetMapping
	@Operation(summary = "Listar las fincas del usuario autenticado con paginacion y busqueda (RF02)")
	public PaginaResponse<FincaResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @Size(max = 100) String buscar,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
		return PaginaResponse.de(
				fincaService.listar(UsuarioAutenticado.id(jwt), buscar,
						Paginaciones.de(page, size, "fechaCreacion")),
				FincaResponse::de);
	}

	@GetMapping("/{fincaId}")
	@Operation(summary = "Consultar una finca (RF02)")
	public FincaResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fincaId) {
		return FincaResponse.de(fincaService.delUsuario(fincaId, UsuarioAutenticado.id(jwt)));
	}

	@PutMapping("/{fincaId}")
	@Operation(summary = "Actualizar los datos de una finca (RF02)")
	public FincaResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fincaId,
			@Valid @RequestBody FincaRequest solicitud) {
		return FincaResponse.de(
				fincaService.actualizar(fincaId, UsuarioAutenticado.id(jwt), solicitud));
	}

	@PostMapping("/{fincaId}/lotes")
	@Operation(summary = "Registrar un lote (parcela) dentro de una finca (RF02)")
	public ResponseEntity<LoteResponse> crearLote(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fincaId, @Valid @RequestBody LoteRequest solicitud) {
		var lote = loteService.crear(fincaId, UsuarioAutenticado.id(jwt), solicitud);
		return ResponseEntity.created(URI.create("/api/v1/lotes/" + lote.getId()))
				.body(LoteResponse.de(lote));
	}

	@GetMapping("/{fincaId}/lotes")
	@Operation(summary = "Listar los lotes de una finca con paginacion y busqueda (RF02)")
	public PaginaResponse<LoteResponse> listarLotes(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fincaId,
			@RequestParam(required = false) @Size(max = 100) String buscar,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
		return PaginaResponse.de(loteService.listarPorFinca(fincaId, UsuarioAutenticado.id(jwt),
				buscar, Paginaciones.de(page, size, "fechaCreacion")), LoteResponse::de);
	}
}
