package com.sisagrodev.agronomico.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LaborTest {

	@Test
	@DisplayName("el costo total suma mano de obra e insumos")
	void sumaManoDeObraEInsumos() {
		Labor labor = new Labor(null);
		labor.setCostoManoObra(new BigDecimal("180000"));
		labor.setCostoInsumos(new BigDecimal("45000"));

		labor.recalcularCostoTotal();

		assertThat(labor.getCostoTotal()).isEqualByComparingTo("225000");
	}

	@Test
	@DisplayName("un costo de insumos ausente cuenta como cero, no como error")
	void tratraInsumosNulosComoCero() {
		Labor labor = new Labor(null);
		labor.setCostoManoObra(new BigDecimal("180000"));
		labor.setCostoInsumos(null);

		labor.recalcularCostoTotal();

		assertThat(labor.getCostoInsumos()).isEqualByComparingTo("0");
		assertThat(labor.getCostoTotal()).isEqualByComparingTo("180000");
	}
}
