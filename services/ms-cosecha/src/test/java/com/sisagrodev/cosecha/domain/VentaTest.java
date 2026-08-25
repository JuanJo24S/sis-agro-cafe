package com.sisagrodev.cosecha.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VentaTest {

	@Test
	@DisplayName("el monto total es cantidad por precio por arroba")
	void recalculaMontoTotal() {
		Venta venta = new Venta(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		venta.setCantidad(new BigDecimal("12.5"));
		venta.setPrecioPorArroba(new BigDecimal("950000"));

		venta.recalcularMontoTotal();

		assertThat(venta.getMontoTotal()).isEqualByComparingTo("11875000");
	}

	@Test
	@DisplayName("una venta nueva no esta anulada")
	void ventaNuevaNoEstaAnulada() {
		Venta venta = new Venta(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(venta.isAnulada()).isFalse();
	}

	@Test
	@DisplayName("el constructor asigna un ID")
	void constructorAsignaId() {
		Venta venta = new Venta(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(venta.getId()).isNotNull();
	}
}
