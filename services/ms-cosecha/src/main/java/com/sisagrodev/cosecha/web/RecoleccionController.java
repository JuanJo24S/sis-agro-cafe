package com.sisagrodev.cosecha.web;

import com.sisagrodev.cosecha.domain.EstadoGrano;
import com.sisagrodev.cosecha.domain.Recoleccion;
import com.sisagrodev.cosecha.seguridad.Autenticacion;
import com.sisagrodev.cosecha.seguridad.UsuarioAutenticado;
import com.sisagrodev.cosecha.service.RecoleccionService;
import com.sisagrodev.cosecha.web.dto.EstadoGranoRequest;
import com.sisagrodev.cosecha.web.dto.PaginaResponse;
import com.sisagrodev.cosecha.web.dto.RecoleccionRequest;
import com.sisagrodev.cosecha.web.dto.RecoleccionResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Recolecciones")
public class RecoleccionController {

	private final RecoleccionService servicio;

	public RecoleccionController(RecoleccionService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/lotes/{loteId}/recolecciones")
	@Operation(summary = "Registrar una jornada de recoleccion (RF05)")
	public ResponseEntity<RecoleccionResponse> registrar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID loteId,
			@RequestHeader(name = "Idempotency-Key", required = false) UUID claveIdempotencia,
			@Valid @RequestBody RecoleccionRequest solicitud) {
		Recoleccion recoleccion = servicio.registrar(loteId, Autenticacion.de(jwt),
				claveIdempotencia, solicitud);
		return ResponseEntity.created(URI.create("/api/v1/recolecciones/" + recoleccion.getId()))
				.body(RecoleccionResponse.de(recoleccion));
	}

	@GetMapping("/lotes/{loteId}/recolecciones")
	@Operation(summary = "Consultar las jornadas de recoleccion de un lote (RF05)")
	public PaginaResponse<RecoleccionResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID loteId,
			@RequestParam(required = false) EstadoGrano estadoGrano,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PaginaResponse.de(servicio.listar(loteId, Autenticacion.de(jwt), estadoGrano, desde,
				hasta, Paginaciones.de(page, size, "fecha")), RecoleccionResponse::de);
	}

	@GetMapping("/recolecciones/{recoleccionId}")
	@Operation(summary = "Consultar una recoleccion (RF05)")
	public RecoleccionResponse obtener(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID recoleccionId) {
		return RecoleccionResponse.de(
				servicio.obtener(recoleccionId, UsuarioAutenticado.id(jwt)));
	}

	@PatchMapping("/recolecciones/{recoleccionId}/estado")
	@Operation(summary = "Avanzar el estado de procesamiento del grano (RF05)")
	public RecoleccionResponse cambiarEstado(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID recoleccionId, @Valid @RequestBody EstadoGranoRequest solicitud) {
		return RecoleccionResponse.de(servicio.cambiarEstado(recoleccionId,
				UsuarioAutenticado.id(jwt), solicitud.estadoGrano()));
	}

	@DeleteMapping("/recolecciones/{recoleccionId}")
	@Operation(summary = "Anular una recoleccion registrada por error")
	public ResponseEntity<Void> anular(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID recoleccionId) {
		servicio.anular(recoleccionId, UsuarioAutenticado.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
