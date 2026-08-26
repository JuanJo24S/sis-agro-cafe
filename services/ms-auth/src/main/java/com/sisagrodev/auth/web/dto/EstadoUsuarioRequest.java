package com.sisagrodev.auth.web.dto;

import com.sisagrodev.auth.domain.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(@NotNull EstadoUsuario estado) {
}
