package com.sisagrodev.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sisagrodev.auth.domain.EstadoUsuario;
import com.sisagrodev.auth.domain.Rol;
import com.sisagrodev.auth.domain.Usuario;
import com.sisagrodev.auth.error.ExcepcionNegocio;
import com.sisagrodev.auth.repository.UsuarioRepository;
import com.sisagrodev.auth.web.dto.LoginRequest;
import com.sisagrodev.auth.web.dto.RegistroRequest;
import com.sisagrodev.auth.web.dto.TokenResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository repositorio;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	private UsuarioService servicio;

	@BeforeEach
	void setUp() {
		servicio = new UsuarioService(repositorio, passwordEncoder, jwtService);
	}

	@Test
	@DisplayName("registrar con email duplicado lanza conflicto")
	void registrarConEmailDuplicadoLanzaConflicto() {
		RegistroRequest solicitud = new RegistroRequest("Carlos Ruiz", "carlos@finca.com", "password123", Rol.CAFICULTOR);
		when(repositorio.existsByEmailIgnoreCase("carlos@finca.com")).thenReturn(true);

		assertThatThrownBy(() -> servicio.registrar(solicitud))
				.isInstanceOf(ExcepcionNegocio.class)
				.satisfies(error -> {
					ExcepcionNegocio ex = (ExcepcionNegocio) error;
					assertThat(ex.getEstado()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(ex.getMessage()).isEqualTo("El correo carlos@finca.com ya esta registrado.");
				});

		verify(repositorio, never()).save(any());
	}

	@Test
	@DisplayName("registrar usuario nuevo lo guarda en el repositorio")
	void registrarUsuarioNuevoLoGuardaEnRepositorio() {
		RegistroRequest solicitud = new RegistroRequest("Carlos Ruiz", "Carlos@Finca.com", "password123", Rol.CAFICULTOR);
		when(repositorio.existsByEmailIgnoreCase("Carlos@Finca.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hash_encriptado");
		when(repositorio.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Usuario resultado = servicio.registrar(solicitud);

		assertThat(resultado).isNotNull();
		assertThat(resultado.getNombre()).isEqualTo("Carlos Ruiz");
		assertThat(resultado.getEmail()).isEqualTo("carlos@finca.com");
		assertThat(resultado.getPasswordHash()).isEqualTo("hash_encriptado");
		assertThat(resultado.getRol()).isEqualTo(Rol.CAFICULTOR);
		assertThat(resultado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
		assertThat(resultado.getId()).isNotNull();
		assertThat(resultado.getFechaCreacion()).isNotNull();

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(repositorio).save(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo("carlos@finca.com");
		assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash_encriptado");
	}

	@Test
	@DisplayName("autenticar con credenciales correctas retorna token")
	void autenticarConCredencialesCorrectasRetornaToken() {
		LoginRequest solicitud = new LoginRequest("carlos@finca.com", "password123");
		Usuario usuario = new Usuario("Carlos Ruiz", "carlos@finca.com", "hash_encriptado", Rol.CAFICULTOR);
		when(repositorio.findByEmailIgnoreCase("carlos@finca.com")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("password123", "hash_encriptado")).thenReturn(true);
		when(jwtService.emitir(usuario)).thenReturn("jwt-token-prueba");
		when(jwtService.expiracionSegundos()).thenReturn(3600L);

		TokenResponse respuesta = servicio.autenticar(solicitud);

		assertThat(respuesta).isNotNull();
		assertThat(respuesta.accessToken()).isEqualTo("jwt-token-prueba");
		assertThat(respuesta.tokenType()).isEqualTo("Bearer");
		assertThat(respuesta.expiresIn()).isEqualTo(3600L);
	}

	@Test
	@DisplayName("autenticar con contrasena incorrecta lanza error")
	void autenticarConContrasenaIncorrectaLanzaError() {
		LoginRequest solicitud = new LoginRequest("carlos@finca.com", "passwordErroneo");
		Usuario usuario = new Usuario("Carlos Ruiz", "carlos@finca.com", "hash_encriptado", Rol.CAFICULTOR);
		when(repositorio.findByEmailIgnoreCase("carlos@finca.com")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("passwordErroneo", "hash_encriptado")).thenReturn(false);

		assertThatThrownBy(() -> servicio.autenticar(solicitud))
				.isInstanceOf(ExcepcionNegocio.class)
				.satisfies(error -> {
					ExcepcionNegocio ex = (ExcepcionNegocio) error;
					assertThat(ex.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(ex.getMessage()).isEqualTo("Correo o contrasena incorrectos.");
				});
	}

	@Test
	@DisplayName("autenticar usuario inactivo lanza prohibido")
	void autenticarUsuarioInactivoLanzaProhibido() {
		LoginRequest solicitud = new LoginRequest("carlos@finca.com", "password123");
		Usuario usuario = new Usuario("Carlos Ruiz", "carlos@finca.com", "hash_encriptado", Rol.CAFICULTOR);
		usuario.setEstado(EstadoUsuario.INACTIVO);
		when(repositorio.findByEmailIgnoreCase("carlos@finca.com")).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("password123", "hash_encriptado")).thenReturn(true);

		assertThatThrownBy(() -> servicio.autenticar(solicitud))
				.isInstanceOf(ExcepcionNegocio.class)
				.satisfies(error -> {
					ExcepcionNegocio ex = (ExcepcionNegocio) error;
					assertThat(ex.getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
					assertThat(ex.getMessage()).isEqualTo("La cuenta esta desactivada.");
				});
	}
}
