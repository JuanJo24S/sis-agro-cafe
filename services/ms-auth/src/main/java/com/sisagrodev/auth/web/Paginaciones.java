package com.sisagrodev.auth.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Traduce los parametros `page`/`size` del contrato a un Pageable de Spring Data. */
final class Paginaciones {

	private static final int TAMANO_MAXIMO = 20;

	private Paginaciones() {
	}

	static Pageable de(int page, int size, String campoOrden) {
		int tamano = Math.min(Math.max(size, 1), TAMANO_MAXIMO);
		return PageRequest.of(Math.max(page, 0), tamano, Sort.by(campoOrden).descending());
	}
}
