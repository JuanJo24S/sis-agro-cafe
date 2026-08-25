package com.sisagrodev.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Firma (HS256) y verificacion del JWT con un secreto compartido. */
@Configuration
@EnableConfigurationProperties(PropiedadesJwt.class)
public class JwtConfig {

	@Bean
	public SecretKey claveJwt(PropiedadesJwt propiedades) {
		byte[] secreto = propiedades.secret().getBytes(StandardCharsets.UTF_8);
		if (secreto.length < 32) {
			throw new IllegalStateException(
					"agrocafe.jwt.secret debe tener al menos 32 caracteres para HS256.");
		}
		return new SecretKeySpec(secreto, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey claveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey claveJwt) {
		return NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
	}
}
