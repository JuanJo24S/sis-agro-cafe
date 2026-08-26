package com.sisagrodev.auth.error;

import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a respuestas `application/problem+json` (RFC 9457).
 * Va primero que el manejador por defecto de Spring Boot para conservar el arreglo
 * `errores` que define el contrato OpenAPI.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorErrores {

	public record ErrorCampo(String campo, String mensaje) {
	}

	@ExceptionHandler(ExcepcionNegocio.class)
	public ResponseEntity<ProblemDetail> negocio(ExcepcionNegocio ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.getEstado(), ex.getMessage());
		problema.setTitle(ex.getTitulo());
		return ResponseEntity.status(ex.getEstado()).body(problema);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex) {
		List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
				.map(this::aErrorCampo)
				.toList();
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "Uno o mas campos de la solicitud son invalidos.");
		problema.setTitle("Solicitud invalida");
		problema.setProperty("errores", errores);
		return ResponseEntity.badRequest().body(problema);
	}

	private ErrorCampo aErrorCampo(FieldError error) {
		return new ErrorCampo(error.getField(), error.getDefaultMessage());
	}
}
