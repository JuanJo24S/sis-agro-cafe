package com.sisagrodev.auth.web.dto;

import com.sisagrodev.auth.domain.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Email @Size(max = 180) String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotNull Rol rol) {
}
