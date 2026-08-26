package com.sisagrodev.cosecha.cliente;

import java.time.LocalDate;
import java.util.UUID;

/** Vista minima del lote que ms-cosecha necesita de ms-agronomico. */
public record LoteRef(UUID id, UUID fincaId, String nombre, LocalDate fechaSiembra) {
}
