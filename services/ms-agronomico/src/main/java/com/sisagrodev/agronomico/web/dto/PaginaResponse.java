package com.sisagrodev.agronomico.web.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Envoltura de paginacion con la forma definida en el contrato OpenAPI. */
public record PaginaResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapeo) {
		return new PaginaResponse<>(
				pagina.getContent().stream().map(mapeo).toList(),
				pagina.getNumber(),
				pagina.getSize(),
				pagina.getTotalElements(),
				pagina.getTotalPages());
	}
}
