package com.sisagrodev.agronomico.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FertilizacionTest {

	@Test
	@DisplayName("el costo total suma insumos y mano de obra")
	void sumaInsumosYManoDeObra() {
		Fertilizacion fertilizacion = new Fertilizacion(null);
		fertilizacion.setCostoInsumos(new BigDecimal("120000"));
		fertilizacion.setCostoManoObra(new BigDecimal("80000"));

		fertilizacion.recalcularCostoTotal();

		assertThat(fertilizacion.getCostoTotal()).isEqualByComparingTo("200000");
	}

	@Test
	@DisplayName("insumos nulos se tratan como cero")
	void insumosNulosSeTratanComoCero() {
		Fertilizacion fertilizacion = new Fertilizacion(null);
		fertilizacion.setCostoInsumos(null);
		fertilizacion.setCostoManoObra(new BigDecimal("80000"));

		fertilizacion.recalcularCostoTotal();

		assertThat(fertilizacion.getCostoInsumos()).isEqualByComparingTo("0");
		assertThat(fertilizacion.getCostoTotal()).isEqualByComparingTo("80000");
	}

	@Test
	@DisplayName("mano de obra nula se trata como cero")
	void manoDeObraNulaSeTrataComoCero() {
		Fertilizacion fertilizacion = new Fertilizacion(null);
		fertilizacion.setCostoInsumos(new BigDecimal("120000"));
		fertilizacion.setCostoManoObra(null);

		fertilizacion.recalcularCostoTotal();

		assertThat(fertilizacion.getCostoManoObra()).isEqualByComparingTo("0");
		assertThat(fertilizacion.getCostoTotal()).isEqualByComparingTo("120000");
	}
}
