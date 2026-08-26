package com.sisagrodev.cosecha.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EstadoGranoTest {

	@Test
	@DisplayName("el grano avanza en el orden del proceso post-cosecha")
	void avanzaHaciaAdelante() {
		assertThat(EstadoGrano.CEREZA.puedeAvanzarA(EstadoGrano.MOJADO)).isTrue();
		assertThat(EstadoGrano.MOJADO.puedeAvanzarA(EstadoGrano.ALMACENADO)).isTrue();
	}

	@Test
	@DisplayName("el grano nunca retrocede de estado")
	void noRetrocede() {
		assertThat(EstadoGrano.ALMACENADO.puedeAvanzarA(EstadoGrano.CEREZA)).isFalse();
		assertThat(EstadoGrano.SECO_TRILLA.puedeAvanzarA(EstadoGrano.SECO_AGUA)).isFalse();
	}

	@Test
	@DisplayName("quedarse en el mismo estado no es un avance valido")
	void noAvanzaAlMismoEstado() {
		assertThat(EstadoGrano.MOJADO.puedeAvanzarA(EstadoGrano.MOJADO)).isFalse();
	}
}
