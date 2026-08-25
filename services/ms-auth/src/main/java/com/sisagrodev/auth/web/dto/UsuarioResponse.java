package com.sisagrodev.auth.web.dto;

import com.sisagrodev.auth.domain.EstadoUsuario;
import com.sisagrodev.auth.domain.Rol;
import com.sisagrodev.auth.domain.Usuario;
import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
		UUID id,
		String nombre,
		String email,
		Rol rol,
		EstadoUsuario estado,
		Instant fechaCreacion) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getRol(),
				usuario.getEstado(),
				usuario.getFechaCreacion());
	}
}
