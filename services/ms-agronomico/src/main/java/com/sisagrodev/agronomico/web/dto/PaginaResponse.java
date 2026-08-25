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
		int totalPages,
		boolean first,
		boolean last,
		boolean hasPrevious,
		boolean hasNext) {

	public PaginaResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
		this(content, page, size, totalElements, totalPages, page == 0, page >= totalPages - 1, page > 0, page < totalPages - 1);
	}

	public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapeo) {
		return new PaginaResponse<>(
				pagina.getContent().stream().map(mapeo).toList(),
				pagina.getNumber(),
				pagina.getSize(),
				pagina.getTotalElements(),
				pagina.getTotalPages(),
				pagina.isFirst(),
				pagina.isLast(),
				pagina.hasPrevious(),
				pagina.hasNext());
	}
}
