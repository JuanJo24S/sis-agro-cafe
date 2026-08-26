package com.sisagrodev.financiero.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sisagrodev.financiero.domain.BalanceLote;
import com.sisagrodev.financiero.domain.EventoProcesado;
import com.sisagrodev.financiero.evento.MovimientoFinancieroEvent;
import com.sisagrodev.financiero.evento.Operacion;
import com.sisagrodev.financiero.evento.OrigenMovimiento;
import com.sisagrodev.financiero.evento.TipoMovimiento;
import com.sisagrodev.financiero.repository.BalanceLoteRepository;
import com.sisagrodev.financiero.repository.EventoProcesadoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

	private static final UUID LOTE = UUID.randomUUID();
	private static final UUID FINCA = UUID.randomUUID();
	private static final UUID USUARIO = UUID.randomUUID();

	@Mock
	private BalanceLoteRepository balances;

	@Mock
	private EventoProcesadoRepository eventosProcesados;

	private MovimientoFinancieroEvent evento(TipoMovimiento tipo, Operacion operacion,
			String monto) {
		return new MovimientoFinancieroEvent(UUID.randomUUID(), tipo, operacion,
				OrigenMovimiento.LABOR, UUID.randomUUID(), LOTE, FINCA, USUARIO,
				new BigDecimal(monto), LocalDate.now(), Instant.now());
	}

	@Test
	@DisplayName("un costo y un ingreso dejan el balance en la diferencia")
	void acumulaCostosEIngresos() {
		BalanceLote balance = new BalanceLote(LOTE, FINCA, USUARIO);
		when(balances.findById(LOTE)).thenReturn(Optional.of(balance));
		when(eventosProcesados.existsById(any())).thenReturn(false);
		BalanceService servicio = new BalanceService(balances, eventosProcesados);

		servicio.aplicar(evento(TipoMovimiento.COSTO, Operacion.REGISTRO, "225000"), "costos");
		servicio.aplicar(evento(TipoMovimiento.INGRESO, Operacion.REGISTRO, "1000000"), "ingresos");

		assertThat(balance.getCostoTotal()).isEqualByComparingTo("225000");
		assertThat(balance.getIngresoTotal()).isEqualByComparingTo("1000000");
		assertThat(balance.balance()).isEqualByComparingTo("775000");
	}

	@Test
	@DisplayName("una anulacion descuenta el monto que se habia sumado")
	void compensaConLaAnulacion() {
		BalanceLote balance = new BalanceLote(LOTE, FINCA, USUARIO);
		when(balances.findById(LOTE)).thenReturn(Optional.of(balance));
		when(eventosProcesados.existsById(any())).thenReturn(false);
		BalanceService servicio = new BalanceService(balances, eventosProcesados);

		servicio.aplicar(evento(TipoMovimiento.COSTO, Operacion.REGISTRO, "225000"), "costos");
		servicio.aplicar(evento(TipoMovimiento.COSTO, Operacion.ANULACION, "225000"), "costos");

		assertThat(balance.getCostoTotal()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("un evento que ya se proceso no se vuelve a sumar")
	void ignoraEventosRepetidos() {
		when(eventosProcesados.existsById(any())).thenReturn(true);
		BalanceService servicio = new BalanceService(balances, eventosProcesados);

		servicio.aplicar(evento(TipoMovimiento.COSTO, Operacion.REGISTRO, "225000"), "costos");

		verify(balances, never()).save(any());
	}

	@Test
	@DisplayName("el primer movimiento de un lote crea su balance")
	void creaElBalanceCuandoNoExiste() {
		when(balances.findById(LOTE)).thenReturn(Optional.empty());
		when(eventosProcesados.existsById(any())).thenReturn(false);
		BalanceService servicio = new BalanceService(balances, eventosProcesados);

		servicio.aplicar(evento(TipoMovimiento.INGRESO, Operacion.REGISTRO, "500000"), "ingresos");

		ArgumentCaptor<BalanceLote> capturado = ArgumentCaptor.forClass(BalanceLote.class);
		verify(balances).save(capturado.capture());
		assertThat(capturado.getValue().getIngresoTotal()).isEqualByComparingTo("500000");
		assertThat(capturado.getValue().getFincaId()).isEqualTo(FINCA);
		verify(eventosProcesados).save(any(EventoProcesado.class));
	}
}
