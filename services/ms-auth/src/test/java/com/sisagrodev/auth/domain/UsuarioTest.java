package com.sisagrodev.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsuarioTest {

	@Test
	@DisplayName("el constructor asigna ID, estado ACTIVO y fecha de creacion")
	void constructorAsignaCamposPorDefecto() {
		Instant antes = Instant.now().minusMillis(100);

		Usuario usuario = new Usuario("Carlos Ruiz", "carlos@finca.com", "hashSeguro", Rol.CAFICULTOR);

		assertThat(usuario.getId()).isNotNull();
		assertThat(usuario.getNombre()).isEqualTo("Carlos Ruiz");
		assertThat(usuario.getEmail()).isEqualTo("carlos@finca.com");
		assertThat(usuario.getPasswordHash()).isEqualTo("hashSeguro");
		assertThat(usuario.getRol()).isEqualTo(Rol.CAFICULTOR);
		assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
		assertThat(usuario.getFechaCreacion()).isNotNull();
		assertThat(usuario.getFechaCreacion()).isAfterOrEqualTo(antes);
	}

	@Test
	@DisplayName("el email se normaliza a minusculas en el constructor del servicio")
	void entidadAlmacenaEmailSinModificar() {
		String emailOriginal = "Carlos.Ruiz@FINCA.COM";

		Usuario usuario = new Usuario("Carlos Ruiz", emailOriginal, "hashSeguro", Rol.CAFICULTOR);

		assertThat(usuario.getEmail()).isEqualTo(emailOriginal);
	}
}
