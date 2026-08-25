package com.sisagrodev.auth.web;

import com.sisagrodev.auth.domain.Usuario;
import com.sisagrodev.auth.service.UsuarioService;
import com.sisagrodev.auth.web.dto.CambiarPasswordRequest;
import com.sisagrodev.auth.web.dto.LoginRequest;
import com.sisagrodev.auth.web.dto.RegistroRequest;
import com.sisagrodev.auth.web.dto.TokenResponse;
import com.sisagrodev.auth.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

	private final UsuarioService servicio;

	public AuthController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/registro")
	@SecurityRequirements
	@Operation(summary = "Registrar un nuevo usuario (RF01)")
	public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest solicitud) {
		Usuario usuario = servicio.registrar(solicitud);
		return ResponseEntity
				.created(URI.create("/api/v1/usuarios/" + usuario.getId()))
				.body(UsuarioResponse.de(usuario));
	}

	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Autenticar credenciales y obtener un token JWT (RF01)")
	public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
		return servicio.autenticar(solicitud);
	}

	@PostMapping("/cambiar-password")
	@Operation(summary = "Cambiar la contrasena del usuario autenticado")
	public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CambiarPasswordRequest solicitud) {
		servicio.cambiarPassword(UUID.fromString(jwt.getSubject()), solicitud);
		return ResponseEntity.noContent().build();
	}
}
