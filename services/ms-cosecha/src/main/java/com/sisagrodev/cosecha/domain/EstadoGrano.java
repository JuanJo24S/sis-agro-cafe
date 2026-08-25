package com.sisagrodev.cosecha.domain;

import java.util.List;

/** Trazabilidad post-cosecha: el grano solo avanza en este orden (RF05). */
public enum EstadoGrano {

	CEREZA,
	MOJADO,
	SECO_AGUA,
	SECO_TRILLA,
	ALMACENADO;

	private static final List<EstadoGrano> ORDEN = List.of(
			CEREZA, MOJADO, SECO_AGUA, SECO_TRILLA, ALMACENADO);

	public boolean puedeAvanzarA(EstadoGrano destino) {
		return ORDEN.indexOf(destino) > ORDEN.indexOf(this);
	}
}
