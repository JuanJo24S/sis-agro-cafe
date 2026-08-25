package com.sisagrodev.auth.service;

import com.sisagrodev.auth.config.PropiedadesJwt;
import com.sisagrodev.auth.domain.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite el JWT que los demas microservicios validan localmente. */
@Service
public class JwtService {

	private final JwtEncoder encoder;
	private final PropiedadesJwt propiedades;

	public JwtService(JwtEncoder encoder, PropiedadesJwt propiedades) {
		this.encoder = encoder;
		this.propiedades = propiedades;
	}

	public String emitir(Usuario usuario) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(propiedades.emisor())
				.issuedAt(ahora)
				.expiresAt(ahora.plus(propiedades.expiracionSegundos(), ChronoUnit.SECONDS))
				.subject(usuario.getId().toString())
				.claim("email", usuario.getEmail())
				.claim("nombre", usuario.getNombre())
				.claim("rol", usuario.getRol().name())
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
	}

	public long expiracionSegundos() {
		return propiedades.expiracionSegundos();
	}
}
