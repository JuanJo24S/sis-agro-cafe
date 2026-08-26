package com.sisagrodev.cosecha.service;

import com.sisagrodev.cosecha.cliente.ClienteAgronomico;
import com.sisagrodev.cosecha.cliente.LoteRef;
import com.sisagrodev.cosecha.domain.EstadoGrano;
import com.sisagrodev.cosecha.domain.Recoleccion;
import com.sisagrodev.cosecha.error.ExcepcionNegocio;
import com.sisagrodev.cosecha.evento.MovimientoFinancieroEvent;
import com.sisagrodev.cosecha.evento.Operacion;
import com.sisagrodev.cosecha.evento.OrigenMovimiento;
import com.sisagrodev.cosecha.evento.PublicadorMovimientos;
import com.sisagrodev.cosecha.evento.TipoMovimiento;
import com.sisagrodev.cosecha.repository.Filtros;
import com.sisagrodev.cosecha.repository.RecoleccionRepository;
import com.sisagrodev.cosecha.seguridad.Autenticacion;
import com.sisagrodev.cosecha.web.dto.RecoleccionRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoleccionService {

	private static final String RECURSO = "recoleccion";

	private final RecoleccionRepository repositorio;
	private final ClienteAgronomico clienteAgronomico;
	private final IdempotenciaService idempotencia;
	private final PublicadorMovimientos publicador;

	/**
	 * Decision de equipo: el pago a recolectores SI es un costo real del lote, asi que
	 * por defecto se publica como `CostoRegistrado`. Se puede apagar para volver al
	 * comportamiento literal del contrato original (la recoleccion no genera costo).
	 */
	private final boolean publicarCostoRecoleccion;

	public RecoleccionService(RecoleccionRepository repositorio,
			ClienteAgronomico clienteAgronomico, IdempotenciaService idempotencia,
			PublicadorMovimientos publicador,
			@Value("${agrocafe.publicar-costo-recoleccion:true}") boolean publicarCostoRecoleccion) {
		this.repositorio = repositorio;
		this.clienteAgronomico = clienteAgronomico;
		this.idempotencia = idempotencia;
		this.publicador = publicador;
		this.publicarCostoRecoleccion = publicarCostoRecoleccion;
	}

	@Transactional
	public Recoleccion registrar(UUID loteId, Autenticacion auth, UUID claveIdempotencia,
			RecoleccionRequest solicitud) {
		var previa = idempotencia.recursoPrevio(claveIdempotencia, RECURSO);
		if (previa.isPresent()) {
			return obtener(previa.get(), auth.usuarioId());
		}

		LoteRef lote = clienteAgronomico.obtenerLote(loteId, auth.token());
		Recoleccion recoleccion = new Recoleccion(lote.id(), lote.fincaId(), auth.usuarioId());
		recoleccion.setFecha(solicitud.fecha());
		recoleccion.setPesoCascaraKg(solicitud.pesoCascaraKg());
		recoleccion.setValorPorKgArroba(solicitud.valorPorKgArroba());
		recoleccion.setRecolector(solicitud.recolector());
		if (solicitud.estadoGrano() != null) {
			recoleccion.setEstadoGrano(solicitud.estadoGrano());
		}
		recoleccion.recalcularCostoManoObra();
		repositorio.save(recoleccion);

		idempotencia.registrar(claveIdempotencia, RECURSO, recoleccion.getId(), auth.usuarioId());
		if (publicarCostoRecoleccion) {
			publicarCosto(recoleccion, Operacion.REGISTRO);
		}
		return recoleccion;
	}

	@Transactional(readOnly = true)
	public Page<Recoleccion> listar(UUID loteId, Autenticacion auth, EstadoGrano estadoGrano,
			LocalDate desde, LocalDate hasta, Pageable paginacion) {
		clienteAgronomico.obtenerLote(loteId, auth.token());
		return repositorio.findAll(Filtros.recolecciones(loteId, estadoGrano, desde, hasta),
				paginacion);
	}

	@Transactional(readOnly = true)
	public Recoleccion obtener(UUID recoleccionId, UUID usuarioId) {
		Recoleccion recoleccion = repositorio.findById(recoleccionId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado(
						"La recoleccion %s no existe.".formatted(recoleccionId)));
		if (!recoleccion.getUsuarioId().equals(usuarioId)) {
			throw ExcepcionNegocio.prohibido("La recoleccion no pertenece al usuario autenticado.");
		}
		return recoleccion;
	}

	/** Avance de la trazabilidad del grano; no se permite retroceder de estado. */
	@Transactional
	public Recoleccion cambiarEstado(UUID recoleccionId, UUID usuarioId, EstadoGrano destino) {
		Recoleccion recoleccion = obtener(recoleccionId, usuarioId);
		if (recoleccion.getEstadoGrano() == destino) {
			return recoleccion;
		}
		if (!recoleccion.getEstadoGrano().puedeAvanzarA(destino)) {
			throw ExcepcionNegocio.conflicto(
					"No se permite pasar de %s a %s.".formatted(recoleccion.getEstadoGrano(), destino));
		}
		recoleccion.setEstadoGrano(destino);
		return recoleccion;
	}

	@Transactional
	public void anular(UUID recoleccionId, UUID usuarioId) {
		Recoleccion recoleccion = obtener(recoleccionId, usuarioId);
		if (recoleccion.isAnulada()) {
			return;
		}
		recoleccion.setAnulada(true);
		if (publicarCostoRecoleccion) {
			publicarCosto(recoleccion, Operacion.ANULACION);
		}
	}

	private void publicarCosto(Recoleccion recoleccion, Operacion operacion) {
		publicador.publicar(MovimientoFinancieroEvent.de(TipoMovimiento.COSTO, operacion,
				OrigenMovimiento.RECOLECCION, recoleccion.getId(), recoleccion.getLoteId(),
				recoleccion.getFincaId(), recoleccion.getUsuarioId(),
				recoleccion.getCostoManoObra(), recoleccion.getFecha()));
	}
}
