package com.sisagrodev.agronomico.evento;

public enum Operacion {
	/** Suma el monto al acumulado del balance. */
	REGISTRO,
	/** Compensa (resta) un monto publicado antes por error. */
	ANULACION
}
