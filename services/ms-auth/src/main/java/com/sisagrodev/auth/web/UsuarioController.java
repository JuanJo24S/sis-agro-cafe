package com.sisagrodev.auth.web;

import com.sisagrodev.auth.service.UsuarioService;
import com.sisagrodev.auth.web.dto.ActualizarPerfilRequest;
import com.sisagrodev.auth.web.dto.EstadoUsuarioRequest;
import com.sisagrodev.auth.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {

	private final UsuarioService servicio;

	public UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/me")
	@Operation(summary = "Consultar el perfil del usuario autenticado")
	public UsuarioResponse perfil(@AuthenticationPrincipal Jwt jwt) {
		return UsuarioResponse.de(servicio.porId(idDe(jwt)));
	}

	@PutMapping("/me")
	@Operation(summary = "Actualizar el perfil del usuario autenticado")
	public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ActualizarPerfilRequest solicitud) {
		return UsuarioResponse.de(servicio.actualizarPerfil(idDe(jwt), solicitud));
	}

	@PatchMapping("/me/estado")
	@Operation(summary = "Desactivar o reactivar la propia cuenta (baja logica, RF01)")
	public UsuarioResponse cambiarEstado(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody EstadoUsuarioRequest solicitud) {
		return UsuarioResponse.de(servicio.cambiarEstado(idDe(jwt), solicitud));
	}

	private UUID idDe(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
