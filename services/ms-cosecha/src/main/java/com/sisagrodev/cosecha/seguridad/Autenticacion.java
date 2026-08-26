package com.sisagrodev.cosecha.seguridad;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identidad del caficultor + el token original, que se propaga a ms-agronomico. */
public record Autenticacion(UUID usuarioId, String token) {

	public static Autenticacion de(Jwt jwt) {
		return new Autenticacion(UsuarioAutenticado.id(jwt), jwt.getTokenValue());
	}
}
