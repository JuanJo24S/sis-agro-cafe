package com.sisagrodev.agronomico.config;

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
						.title("ms-agronomico API")
						.version("1.0.0")
						.description("Fincas, lotes, labores culturales y fertilizaciones. Publica CostoRegistrado en Kafka (RF02-RF04)."))
				.addSecurityItem(new SecurityRequirement().addList(esquema))
				.components(new Components().addSecuritySchemes(esquema, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}
