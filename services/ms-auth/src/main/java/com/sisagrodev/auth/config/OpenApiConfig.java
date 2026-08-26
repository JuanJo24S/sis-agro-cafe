package com.sisagrodev.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openApi() {
		final String esquema = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("ms-auth API")
						.version("1.0.0")
						.description("Identidad y acceso: registro, login y emision de JWT (RF01, RNF02)."))
				.addSecurityItem(new SecurityRequirement().addList(esquema))
				.components(new Components().addSecuritySchemes(esquema, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}
