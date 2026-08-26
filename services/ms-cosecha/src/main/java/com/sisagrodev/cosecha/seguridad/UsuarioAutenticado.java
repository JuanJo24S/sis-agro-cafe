package com.sisagrodev.cosecha.seguridad;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lee la identidad del caficultor desde el JWT ya validado. */
public final class UsuarioAutenticado {

	private UsuarioAutenticado() {
	}

	public static UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	public static String email(Jwt jwt) {
		return jwt.getClaimAsString("email");
	}
}
