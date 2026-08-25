package com.sisagrodev.financiero.service;

import com.sisagrodev.financiero.domain.BalanceLote;
import com.sisagrodev.financiero.domain.EventoProcesado;
import com.sisagrodev.financiero.error.ExcepcionNegocio;
import com.sisagrodev.financiero.evento.MovimientoFinancieroEvent;
import com.sisagrodev.financiero.evento.Operacion;
import com.sisagrodev.financiero.evento.TipoMovimiento;
import com.sisagrodev.financiero.repository.BalanceLoteRepository;
import com.sisagrodev.financiero.repository.EventoProcesadoRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService {

	private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

	private final BalanceLoteRepository balances;
	private final EventoProcesadoRepository eventosProcesados;

	public BalanceService(BalanceLoteRepository balances,
			EventoProcesadoRepository eventosProcesados) {
		this.balances = balances;
		this.eventosProcesados = eventosProcesados;
	}

	/**
	 * Aplica un movimiento al balance del lote. Es idempotente: si el `eventoId` ya se
	 * proceso antes (reintento de Kafka), se ignora.
	 */
	@Transactional
	public void aplicar(MovimientoFinancieroEvent evento, String topico) {
		if (evento.eventoId() == null || evento.loteId() == null || evento.monto() == null) {
			log.warn("Evento descartado por venir incompleto: {}", evento);
			return;
		}
		if (eventosProcesados.existsById(evento.eventoId())) {
			log.debug("Evento {} ya procesado, se ignora", evento.eventoId());
			return;
		}

		BalanceLote balance = balances.findById(evento.loteId())
				.orElseGet(() -> new BalanceLote(evento.loteId(), evento.fincaId(),
						evento.usuarioId()));

		BigDecimal monto = evento.operacion() == Operacion.ANULACION
				? evento.monto().negate()
				: evento.monto();

		if (evento.tipo() == TipoMovimiento.COSTO) {
			balance.sumarCosto(monto);
		}
		else {
			balance.sumarIngreso(monto);
		}

		balances.save(balance);
		eventosProcesados.save(new EventoProcesado(evento.eventoId(), topico));
		log.info("Balance del lote {} actualizado: costos={} ingresos={}", balance.getLoteId(),
				balance.getCostoTotal(), balance.getIngresoTotal());
	}

	@Transactional(readOnly = true)
	public BalanceLote porLote(UUID loteId, UUID usuarioId) {
		BalanceLote balance = balances.findById(loteId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado(
						"El lote %s no tiene movimientos registrados.".formatted(loteId)));
		if (!balance.getUsuarioId().equals(usuarioId)) {
			throw ExcepcionNegocio.prohibido("El balance no pertenece al usuario autenticado.");
		}
		return balance;
	}

	@Transactional(readOnly = true)
	public List<BalanceLote> porLotes(Collection<UUID> loteIds, UUID usuarioId) {
		if (loteIds == null || loteIds.isEmpty()) {
			return List.of();
		}
		return balances.findByLoteIdInAndUsuarioId(loteIds, usuarioId);
	}

	/** Balance de la finca = agregacion de los balances de todos sus lotes. */
	@Transactional(readOnly = true)
	public List<BalanceLote> porFinca(UUID fincaId, UUID usuarioId) {
		List<BalanceLote> lotes = balances.findByFincaIdAndUsuarioId(fincaId, usuarioId);
		if (lotes.isEmpty()) {
			throw ExcepcionNegocio.noEncontrado(
					"La finca %s no tiene movimientos registrados.".formatted(fincaId));
		}
		return lotes;
	}

	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<BalanceLote> porFincaPaginado(UUID fincaId, UUID usuarioId,
			org.springframework.data.domain.Pageable paginacion) {
		return balances.findByFincaIdAndUsuarioId(fincaId, usuarioId, paginacion);
	}

	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<BalanceLote> listarPaginado(UUID usuarioId,
			org.springframework.data.domain.Pageable paginacion) {
		return balances.findByUsuarioId(usuarioId, paginacion);
	}
}
