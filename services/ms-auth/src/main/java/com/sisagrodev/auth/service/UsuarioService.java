package com.sisagrodev.auth.service;

import com.sisagrodev.auth.domain.EstadoUsuario;
import com.sisagrodev.auth.domain.Usuario;
import com.sisagrodev.auth.error.ExcepcionNegocio;
import com.sisagrodev.auth.repository.UsuarioRepository;
import com.sisagrodev.auth.web.dto.ActualizarPerfilRequest;
import com.sisagrodev.auth.web.dto.CambiarPasswordRequest;
import com.sisagrodev.auth.web.dto.EstadoUsuarioRequest;
import com.sisagrodev.auth.web.dto.LoginRequest;
import com.sisagrodev.auth.web.dto.RegistroRequest;
import com.sisagrodev.auth.web.dto.TokenResponse;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private final UsuarioRepository repositorio;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public UsuarioService(UsuarioRepository repositorio, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.repositorio = repositorio;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public Usuario registrar(RegistroRequest solicitud) {
		if (repositorio.existsByEmailIgnoreCase(solicitud.email())) {
			throw ExcepcionNegocio.conflicto("El correo %s ya esta registrado.".formatted(solicitud.email()));
		}
		Usuario usuario = new Usuario(
				solicitud.nombre(),
				solicitud.email().toLowerCase(),
				passwordEncoder.encode(solicitud.password()),
				solicitud.rol());
		return repositorio.save(usuario);
	}

	@Transactional(readOnly = true)
	public TokenResponse autenticar(LoginRequest solicitud) {
		Usuario usuario = repositorio.findByEmailIgnoreCase(solicitud.email())
				.orElseThrow(() -> ExcepcionNegocio.noAutenticado("Correo o contrasena incorrectos."));
		if (!passwordEncoder.matches(solicitud.password(), usuario.getPasswordHash())) {
			throw ExcepcionNegocio.noAutenticado("Correo o contrasena incorrectos.");
		}
		if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
			throw ExcepcionNegocio.prohibido("La cuenta esta desactivada.");
		}
		return TokenResponse.bearer(jwtService.emitir(usuario), jwtService.expiracionSegundos());
	}

	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<Usuario> listar(String buscar,
			org.springframework.data.domain.Pageable paginacion) {
		if (buscar != null && !buscar.isBlank()) {
			return repositorio.buscarPorNombreOEmail(buscar.trim(), paginacion);
		}
		return repositorio.findAll(paginacion);
	}

	@Transactional(readOnly = true)
	public Usuario porId(UUID id) {
		return repositorio.findById(id)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El usuario no existe."));
	}

	@Transactional
	public void cambiarPassword(UUID id, CambiarPasswordRequest solicitud) {
		Usuario usuario = porId(id);
		if (!passwordEncoder.matches(solicitud.passwordActual(), usuario.getPasswordHash())) {
			throw ExcepcionNegocio.reglaDeNegocio("La contrasena actual no coincide.");
		}
		usuario.setPasswordHash(passwordEncoder.encode(solicitud.passwordNueva()));
	}

	@Transactional
	public Usuario actualizarPerfil(UUID id, ActualizarPerfilRequest solicitud) {
		Usuario usuario = porId(id);
		usuario.setNombre(solicitud.nombre());
		return usuario;
	}

	@Transactional
	public Usuario cambiarEstado(UUID id, EstadoUsuarioRequest solicitud) {
		Usuario usuario = porId(id);
		usuario.setEstado(solicitud.estado());
		return usuario;
	}
}
