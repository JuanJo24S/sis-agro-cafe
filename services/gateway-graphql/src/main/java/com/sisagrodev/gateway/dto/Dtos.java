package com.sisagrodev.gateway.dto;

import java.util.List;

/**
 * Vistas de solo lectura de lo que devuelve cada microservicio. El gateway no
 * reinterpreta los datos: los fechas viajan como String ISO-8601 tal cual las expone
 * la API REST, y los enums como su nombre.
 */
public final class Dtos {

	private Dtos() {
	}

	public record Usuario(String id, String nombre, String email, String rol, String estado,
			String fechaCreacion) {
	}

	public record Token(String accessToken, String tokenType, Long expiresIn) {
	}

	public record Finca(String id, String propietarioId, String nombre, String municipio,
			String departamento, Double areaTotalHectareas, String fechaCreacion) {
	}

	public record Lote(String id, String fincaId, String nombre, String variedad,
			String fechaSiembra, Double areaHectareas, Integer densidadSiembra, Integer alturaMsnm,
			Double phSuelo, String tipoSuelo, Double temperaturaPromedio) {
	}

	public record Labor(String id, String loteId, String tipo, String descripcion,
			String fechaEjecucion, Double jornales, Double costoManoObra, Double costoInsumos,
			Double costoTotal, String fechaRegistro) {
	}

	public record Fertilizacion(String id, String loteId, String estado, String fecha,
			String composicion, Double dosisKgPorArbol, Double cantidadTotalKg, Double costoInsumos,
			Double costoManoObra, Double costoTotal, String observaciones, String fechaRegistro) {
	}

	public record Recoleccion(String id, String loteId, String fecha, Double pesoCascaraKg,
			Double valorPorKgArroba, String recolector, String estadoGrano, Double costoManoObra,
			String fechaRegistro) {
	}

	public record Venta(String id, String loteId, String fecha, Double cantidad,
			Double precioPorArroba, String comprador, Double montoTotal, String fechaRegistro) {
	}

	public record Balance(String loteId, String fincaId, Double costoTotal, Double ingresoTotal,
			Double balance, String actualizadoEn) {
	}

	public record Pagina<T>(List<T> content, int page, int size, long totalElements,
			int totalPages) {
	}
}
