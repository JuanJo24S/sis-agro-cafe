package com.sisagrodev.cosecha.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validacion local del JWT emitido por ms-auth: este servicio NO llama a ms-auth en
 * cada peticion, solo verifica la firma con el secreto compartido (RNF02).
 */
@Configuration
public class JwtConfig {

	@Bean
	public JwtDecoder jwtDecoder(@Value("${agrocafe.jwt.secret}") String secreto) {
		byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException(
					"agrocafe.jwt.secret debe tener al menos 32 caracteres para HS256.");
		}
		SecretKey clave = new SecretKeySpec(bytes, "HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
	}
}
