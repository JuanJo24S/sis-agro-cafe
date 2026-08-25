package com.sisagrodev.auth.error;

import org.springframework.http.HttpStatus;

/** Excepcion de negocio que ya conoce el codigo HTTP con el que debe responderse. */
public class ExcepcionNegocio extends RuntimeException {

	private final HttpStatus estado;
	private final String titulo;

	public ExcepcionNegocio(HttpStatus estado, String titulo, String detalle) {
		super(detalle);
		this.estado = estado;
		this.titulo = titulo;
	}

	public HttpStatus getEstado() {
		return estado;
	}

	public String getTitulo() {
		return titulo;
	}

	public static ExcepcionNegocio noEncontrado(String detalle) {
		return new ExcepcionNegocio(HttpStatus.NOT_FOUND, "Recurso no encontrado", detalle);
	}

	public static ExcepcionNegocio conflicto(String detalle) {
		return new ExcepcionNegocio(HttpStatus.CONFLICT, "Conflicto", detalle);
	}

	public static ExcepcionNegocio noAutenticado(String detalle) {
		return new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, "Credenciales invalidas", detalle);
	}

	public static ExcepcionNegocio prohibido(String detalle) {
		return new ExcepcionNegocio(HttpStatus.FORBIDDEN, "Acceso denegado", detalle);
	}

	public static ExcepcionNegocio reglaDeNegocio(String detalle) {
		return new ExcepcionNegocio(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio", detalle);
	}
}
