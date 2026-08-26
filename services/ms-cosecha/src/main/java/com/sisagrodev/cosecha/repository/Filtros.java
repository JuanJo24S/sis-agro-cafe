package com.sisagrodev.cosecha.repository;

import com.sisagrodev.cosecha.domain.EstadoGrano;
import com.sisagrodev.cosecha.domain.Recoleccion;
import com.sisagrodev.cosecha.domain.Venta;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class Filtros {

	private Filtros() {
	}

	public static Specification<Recoleccion> recolecciones(UUID loteId, EstadoGrano estadoGrano,
			LocalDate desde, LocalDate hasta) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.equal(raiz.get("loteId"), loteId));
			condiciones.add(cb.isFalse(raiz.get("anulada")));
			if (estadoGrano != null) {
				condiciones.add(cb.equal(raiz.get("estadoGrano"), estadoGrano));
			}
			if (desde != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<LocalDate>get("fecha"), desde));
			}
			if (hasta != null) {
				condiciones.add(cb.lessThanOrEqualTo(raiz.<LocalDate>get("fecha"), hasta));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}

	public static Specification<Venta> ventas(UUID usuarioId, UUID loteId, LocalDate desde,
			LocalDate hasta) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.equal(raiz.get("usuarioId"), usuarioId));
			condiciones.add(cb.isFalse(raiz.get("anulada")));
			if (loteId != null) {
				condiciones.add(cb.equal(raiz.get("loteId"), loteId));
			}
			if (desde != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<LocalDate>get("fecha"), desde));
			}
			if (hasta != null) {
				condiciones.add(cb.lessThanOrEqualTo(raiz.<LocalDate>get("fecha"), hasta));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}
}
