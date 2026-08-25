package com.sisagrodev.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del token emitido por ms-auth. El mismo `secret` lo comparten los
 * demas microservicios para validar el JWT localmente, sin llamar a ms-auth.
 */
@ConfigurationProperties(prefix = "agrocafe.jwt")
public record PropiedadesJwt(String secret, long expiracionSegundos, String emisor) {
}
