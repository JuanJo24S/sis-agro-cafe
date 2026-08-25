package com.sisagrodev.cosecha.web;

import com.sisagrodev.cosecha.domain.Venta;
import com.sisagrodev.cosecha.seguridad.Autenticacion;
import com.sisagrodev.cosecha.seguridad.UsuarioAutenticado;
import com.sisagrodev.cosecha.service.VentaService;
import com.sisagrodev.cosecha.web.dto.PaginaResponse;
import com.sisagrodev.cosecha.web.dto.VentaRequest;
import com.sisagrodev.cosecha.web.dto.VentaResponse;
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
@RequestMapping("/api/v1/ventas")
@Tag(name = "Ventas")
@Validated
public class VentaController {

	private final VentaService servicio;

	public VentaController(VentaService servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	@Operation(summary = "Registrar una venta; publica IngresoRegistrado en Kafka (RF06)")
	public ResponseEntity<VentaResponse> registrar(@AuthenticationPrincipal Jwt jwt,
			@RequestHeader(name = "Idempotency-Key", required = false) UUID claveIdempotencia,
			@Valid @RequestBody VentaRequest solicitud) {
		Venta venta = servicio.registrar(Autenticacion.de(jwt), claveIdempotencia, solicitud);
		return ResponseEntity.created(URI.create("/api/v1/ventas/" + venta.getId()))
				.body(VentaResponse.de(venta));
	}

	@GetMapping
	@Operation(summary = "Listar ventas del usuario autenticado con paginacion y busqueda (RF06)")
	public PaginaResponse<VentaResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) UUID loteId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) @Size(max = 100) String buscar,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
		return PaginaResponse.de(servicio.listar(UsuarioAutenticado.id(jwt), loteId, desde,
				hasta, buscar, Paginaciones.de(page, size, "fecha")), VentaResponse::de);
	}

	@GetMapping("/{ventaId}")
	@Operation(summary = "Consultar una venta (RF06)")
	public VentaResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ventaId) {
		return VentaResponse.de(servicio.obtener(ventaId, UsuarioAutenticado.id(jwt)));
	}

	@DeleteMapping("/{ventaId}")
	@Operation(summary = "Anular una venta; publica el ingreso compensatorio")
	public ResponseEntity<Void> anular(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ventaId) {
		servicio.anular(ventaId, UsuarioAutenticado.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
