package com.sisagrodev.financiero.config;

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
						.title("ms-financiero API")
						.version("1.0.0")
						.description("Balance consolidado costos vs ingresos por lote y finca, alimentado por eventos Kafka (RF07)."))
				.addSecurityItem(new SecurityRequirement().addList(esquema))
				.components(new Components().addSecuritySchemes(esquema, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}
