package com.sisagrodev.cosecha.service;

import com.sisagrodev.cosecha.cliente.ClienteAgronomico;
import com.sisagrodev.cosecha.cliente.LoteRef;
import com.sisagrodev.cosecha.domain.Venta;
import com.sisagrodev.cosecha.error.ExcepcionNegocio;
import com.sisagrodev.cosecha.evento.MovimientoFinancieroEvent;
import com.sisagrodev.cosecha.evento.Operacion;
import com.sisagrodev.cosecha.evento.OrigenMovimiento;
import com.sisagrodev.cosecha.evento.PublicadorMovimientos;
import com.sisagrodev.cosecha.evento.TipoMovimiento;
import com.sisagrodev.cosecha.repository.Filtros;
import com.sisagrodev.cosecha.repository.VentaRepository;
import com.sisagrodev.cosecha.seguridad.Autenticacion;
import com.sisagrodev.cosecha.web.dto.VentaRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ventas de cafe procesado (RF06); publica `IngresoRegistrado` en Kafka. */
@Service
public class VentaService {

	private static final String RECURSO = "venta";

	private final VentaRepository repositorio;
	private final ClienteAgronomico clienteAgronomico;
	private final IdempotenciaService idempotencia;
	private final PublicadorMovimientos publicador;

	public VentaService(VentaRepository repositorio, ClienteAgronomico clienteAgronomico,
			IdempotenciaService idempotencia, PublicadorMovimientos publicador) {
		this.repositorio = repositorio;
		this.clienteAgronomico = clienteAgronomico;
		this.idempotencia = idempotencia;
		this.publicador = publicador;
	}

	@Transactional
	public Venta registrar(Autenticacion auth, UUID claveIdempotencia, VentaRequest solicitud) {
		var previa = idempotencia.recursoPrevio(claveIdempotencia, RECURSO);
		if (previa.isPresent()) {
			return obtener(previa.get(), auth.usuarioId());
		}

		LoteRef lote = clienteAgronomico.obtenerLote(solicitud.loteId(), auth.token());
		Venta venta = new Venta(lote.id(), lote.fincaId(), auth.usuarioId());
		venta.setFecha(solicitud.fecha());
		venta.setCantidad(solicitud.cantidad());
		venta.setPrecioPorArroba(solicitud.precioPorArroba());
		venta.setComprador(solicitud.comprador());
		venta.recalcularMontoTotal();
		repositorio.save(venta);

		idempotencia.registrar(claveIdempotencia, RECURSO, venta.getId(), auth.usuarioId());
		publicarIngreso(venta, Operacion.REGISTRO);
		return venta;
	}

	@Transactional(readOnly = true)
	public Page<Venta> listar(UUID usuarioId, UUID loteId, LocalDate desde, LocalDate hasta,
			Pageable paginacion) {
		return repositorio.findAll(Filtros.ventas(usuarioId, loteId, desde, hasta), paginacion);
	}

	@Transactional(readOnly = true)
	public Venta obtener(UUID ventaId, UUID usuarioId) {
		Venta venta = repositorio.findById(ventaId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado(
						"La venta %s no existe.".formatted(ventaId)));
		if (!venta.getUsuarioId().equals(usuarioId)) {
			throw ExcepcionNegocio.prohibido("La venta no pertenece al usuario autenticado.");
		}
		return venta;
	}

	/** Anula la venta y publica el ingreso compensatorio para no dejar el balance inflado. */
	@Transactional
	public void anular(UUID ventaId, UUID usuarioId) {
		Venta venta = obtener(ventaId, usuarioId);
		if (venta.isAnulada()) {
			return;
		}
		venta.setAnulada(true);
		publicarIngreso(venta, Operacion.ANULACION);
	}

	private void publicarIngreso(Venta venta, Operacion operacion) {
		publicador.publicar(MovimientoFinancieroEvent.de(TipoMovimiento.INGRESO, operacion,
				OrigenMovimiento.VENTA, venta.getId(), venta.getLoteId(), venta.getFincaId(),
				venta.getUsuarioId(), venta.getMontoTotal(), venta.getFecha()));
	}
}
