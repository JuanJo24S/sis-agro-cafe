package com.sisagrodev.cosecha.web.dto;

import com.sisagrodev.cosecha.domain.EstadoGrano;
import jakarta.validation.constraints.NotNull;

public record EstadoGranoRequest(@NotNull EstadoGrano estadoGrano) {
}
