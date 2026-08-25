package com.sisagrodev.agronomico.web;

import com.sisagrodev.agronomico.domain.Labor;
import com.sisagrodev.agronomico.domain.TipoLabor;
import com.sisagrodev.agronomico.seguridad.UsuarioAutenticado;
import com.sisagrodev.agronomico.service.LaborService;
import com.sisagrodev.agronomico.web.dto.LaborRequest;
import com.sisagrodev.agronomico.web.dto.LaborResponse;
import com.sisagrodev.agronomico.web.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Labores")
@Validated
public class LaborController {

	private final LaborService servicio;

	public LaborController(LaborService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/lotes/{loteId}/labores")
	@Operation(summary = "Registrar una labor cultural ejecutada; publica CostoRegistrado (RF03)")
	public ResponseEntity<LaborResponse> registrar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID loteId,
			@RequestHeader(name = "Idempotency-Key", required = false) UUID claveIdempotencia,
			@Valid @RequestBody LaborRequest solicitud) {
		Labor labor = servicio.registrar(loteId, UsuarioAutenticado.id(jwt), claveIdempotencia,
				solicitud);
		return ResponseEntity.created(URI.create("/api/v1/labores/" + labor.getId()))
				.body(LaborResponse.de(labor));
	}

	@GetMapping("/lotes/{loteId}/labores")
	@Operation(summary = "Consultar el historial de labores de un lote con paginacion y busqueda (RF03)")
	public PaginaResponse<LaborResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID loteId,
			@RequestParam(required = false) TipoLabor tipo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) @Size(max = 100) String buscar,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
		return PaginaResponse.de(servicio.listar(loteId, UsuarioAutenticado.id(jwt), tipo, desde,
				hasta, buscar, Paginaciones.de(page, size, "fechaEjecucion")), LaborResponse::de);
	}

	@GetMapping("/labores/{laborId}")
	@Operation(summary = "Consultar una labor (RF03)")
	public LaborResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID laborId) {
		return LaborResponse.de(servicio.obtener(laborId, UsuarioAutenticado.id(jwt)));
	}

	@DeleteMapping("/labores/{laborId}")
	@Operation(summary = "Anular una labor registrada por error; publica el costo compensatorio")
	public ResponseEntity<Void> anular(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID laborId) {
		servicio.anular(laborId, UsuarioAutenticado.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
