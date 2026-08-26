package com.sisagrodev.agronomico.web;

import com.sisagrodev.agronomico.domain.EstadoFertilizacion;
import com.sisagrodev.agronomico.domain.Fertilizacion;
import com.sisagrodev.agronomico.seguridad.UsuarioAutenticado;
import com.sisagrodev.agronomico.service.FertilizacionService;
import com.sisagrodev.agronomico.web.dto.EjecucionFertilizacionRequest;
import com.sisagrodev.agronomico.web.dto.FertilizacionRequest;
import com.sisagrodev.agronomico.web.dto.FertilizacionResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Fertilizaciones")
public class FertilizacionController {

	private final FertilizacionService servicio;

	public FertilizacionController(FertilizacionService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/lotes/{loteId}/fertilizaciones")
	@Operation(summary = "Registrar (EJECUTADA) o programar (PROGRAMADA) una abonada (RF04)")
	public ResponseEntity<FertilizacionResponse> registrar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID loteId,
			@RequestHeader(name = "Idempotency-Key", required = false) UUID claveIdempotencia,
			@Valid @RequestBody FertilizacionRequest solicitud) {
		Fertilizacion fertilizacion = servicio.registrar(loteId, UsuarioAutenticado.id(jwt),
				claveIdempotencia, solicitud);
		return ResponseEntity
				.created(URI.create("/api/v1/fertilizaciones/" + fertilizacion.getId()))
				.body(FertilizacionResponse.de(fertilizacion));
	}

	@GetMapping("/lotes/{loteId}/fertilizaciones")
	@Operation(summary = "Consultar el historial y el plan de fertilizacion de un lote (RF04)")
	public PaginaResponse<FertilizacionResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID loteId,
			@RequestParam(required = false) EstadoFertilizacion estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PaginaResponse.de(servicio.listar(loteId, UsuarioAutenticado.id(jwt), estado, desde,
				hasta, Paginaciones.de(page, size, "fecha")), FertilizacionResponse::de);
	}

	@GetMapping("/fertilizaciones/{fertilizacionId}")
	@Operation(summary = "Consultar una fertilizacion (RF04)")
	public FertilizacionResponse obtener(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fertilizacionId) {
		return FertilizacionResponse.de(
				servicio.obtener(fertilizacionId, UsuarioAutenticado.id(jwt)));
	}

	@PostMapping("/fertilizaciones/{fertilizacionId}/ejecucion")
	@Operation(summary = "Confirmar la ejecucion de una fertilizacion programada (RF04)")
	public FertilizacionResponse ejecutar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fertilizacionId,
			@RequestHeader(name = "Idempotency-Key", required = false) UUID claveIdempotencia,
			@Valid @RequestBody EjecucionFertilizacionRequest solicitud) {
		return FertilizacionResponse.de(servicio.ejecutar(fertilizacionId,
				UsuarioAutenticado.id(jwt), claveIdempotencia, solicitud));
	}

	@DeleteMapping("/fertilizaciones/{fertilizacionId}")
	@Operation(summary = "Anular una fertilizacion; compensa el costo si ya estaba ejecutada")
	public ResponseEntity<Void> anular(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID fertilizacionId) {
		servicio.anular(fertilizacionId, UsuarioAutenticado.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
