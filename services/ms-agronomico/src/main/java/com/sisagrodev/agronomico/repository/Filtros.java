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
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}

	public static Specification<Fertilizacion> fertilizaciones(UUID loteId,
			EstadoFertilizacion estado, LocalDate desde, LocalDate hasta) {
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
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}
}
