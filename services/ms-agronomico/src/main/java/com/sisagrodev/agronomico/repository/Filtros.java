package com.sisagrodev.agronomico.repository;

import com.sisagrodev.agronomico.domain.EstadoFertilizacion;
import com.sisagrodev.agronomico.domain.Fertilizacion;
import com.sisagrodev.agronomico.domain.Labor;
import com.sisagrodev.agronomico.domain.TipoLabor;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales (tipo/estado y rango de fechas) de los listados por lote. */
public final class Filtros {

	private Filtros() {
	}

	public static Specification<Labor> labores(UUID loteId, TipoLabor tipo, LocalDate desde,
			LocalDate hasta) {
		return labores(loteId, tipo, desde, hasta, null);
	}

	public static Specification<Labor> labores(UUID loteId, TipoLabor tipo, LocalDate desde,
			LocalDate hasta, String buscar) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.equal(raiz.get("loteId"), loteId));
			condiciones.add(cb.isFalse(raiz.get("anulada")));
			if (tipo != null) {
				condiciones.add(cb.equal(raiz.get("tipo"), tipo));
			}
			if (desde != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<LocalDate>get("fechaEjecucion"), desde));
			}
			if (hasta != null) {
				condiciones.add(cb.lessThanOrEqualTo(raiz.<LocalDate>get("fechaEjecucion"), hasta));
			}
			if (buscar != null && !buscar.isBlank()) {
				String patron = "%" + buscar.trim().toLowerCase() + "%";
				condiciones.add(cb.like(cb.lower(raiz.get("descripcion")), patron));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}

	public static Specification<Fertilizacion> fertilizaciones(UUID loteId,
			EstadoFertilizacion estado, LocalDate desde, LocalDate hasta) {
		return fertilizaciones(loteId, estado, desde, hasta, null);
	}

	public static Specification<Fertilizacion> fertilizaciones(UUID loteId,
			EstadoFertilizacion estado, LocalDate desde, LocalDate hasta, String buscar) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.equal(raiz.get("loteId"), loteId));
			if (estado != null) {
				condiciones.add(cb.equal(raiz.get("estado"), estado));
			}
			if (desde != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<LocalDate>get("fecha"), desde));
			}
			if (hasta != null) {
				condiciones.add(cb.lessThanOrEqualTo(raiz.<LocalDate>get("fecha"), hasta));
			}
			if (buscar != null && !buscar.isBlank()) {
				String patron = "%" + buscar.trim().toLowerCase() + "%";
				Predicate enComposicion = cb.like(cb.lower(raiz.get("composicion")), patron);
				Predicate enObs = cb.like(cb.lower(raiz.get("observaciones")), patron);
				condiciones.add(cb.or(enComposicion, enObs));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}
}
