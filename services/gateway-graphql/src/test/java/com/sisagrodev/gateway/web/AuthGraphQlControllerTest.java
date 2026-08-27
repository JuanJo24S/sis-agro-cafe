package com.sisagrodev.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sisagrodev.gateway.dto.Dtos;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AuthGraphQlControllerTest {

	private MockRestServiceServer server;
	private AuthGraphQlController controller;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://ms-auth:8081");
		server = MockRestServiceServer.bindTo(builder).build();
		RestClient cliente = builder.build();
		controller = new AuthGraphQlController(cliente);
	}

	@Test
	@DisplayName("me propaga la cabecera Authorization y deserializa el usuario")
	void mePropagaAutorizacionYRetornaUsuario() {
		server.expect(requestTo("http://ms-auth:8081/api/v1/usuarios/me"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token-123"))
				.andRespond(withSuccess(
						"""
						{
						  "id": "11111111-1111-1111-1111-111111111111",
						  "nombre": "Caficultor Juan",
						  "email": "juan@finca.co",
						  "rol": "CAFICULTOR",
						  "estado": "ACTIVO",
						  "fechaCreacion": "2026-01-01T00:00:00Z"
						}
						""",
						MediaType.APPLICATION_JSON));

		Dtos.Usuario usuario = controller.me("Bearer jwt-token-123");

		server.verify();
		assertThat(usuario).isNotNull();
		assertThat(usuario.email()).isEqualTo("juan@finca.co");
		assertThat(usuario.nombre()).isEqualTo("Caficultor Juan");
	}

	@Test
	@DisplayName("registro envia payload y retorna el usuario creado")
	void registroPublicaPayload() {
		server.expect(requestTo("http://ms-auth:8081/api/v1/auth/registro"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.nombre").value("Maria"))
				.andExpect(jsonPath("$.email").value("maria@finca.co"))
				.andRespond(withSuccess(
						"""
						{
						  "id": "22222222-2222-2222-2222-222222222222",
						  "nombre": "Maria",
						  "email": "maria@finca.co",
						  "rol": "CAFICULTOR",
						  "estado": "ACTIVO",
						  "fechaCreacion": "2026-01-01T00:00:00Z"
						}
						""",
						MediaType.APPLICATION_JSON));

		Dtos.Usuario usuario = controller.registro(Map.of(
				"nombre", "Maria",
				"email", "maria@finca.co",
				"password", "claveSegura123",
				"rol", "CAFICULTOR"));

		server.verify();
		assertThat(usuario).isNotNull();
		assertThat(usuario.email()).isEqualTo("maria@finca.co");
	}

	@Test
	@DisplayName("login publica credenciales y retorna token")
	void loginPublicaCredencialesYRetornaToken() {
		server.expect(requestTo("http://ms-auth:8081/api/v1/auth/login"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.email").value("maria@finca.co"))
				.andRespond(withSuccess(
						"""
						{
						  "accessToken": "ey.token.jwt",
						  "tokenType": "Bearer",
						  "expiresIn": 3600
						}
						""",
						MediaType.APPLICATION_JSON));

		Dtos.Token token = controller.login(Map.of(
				"email", "maria@finca.co",
				"password", "claveSegura123"));

		server.verify();
		assertThat(token).isNotNull();
		assertThat(token.accessToken()).isEqualTo("ey.token.jwt");
		assertThat(token.tokenType()).isEqualTo("Bearer");
	}

	@Test
	@DisplayName("cambiarPassword propaga Authorization y cuerpo hacia ms-auth")
	void cambiarPasswordPropagaAutorizacion() {
		server.expect(requestTo("http://ms-auth:8081/api/v1/auth/cambiar-password"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt-123"))
				.andExpect(jsonPath("$.passwordActual").value("vieja123"))
				.andExpect(jsonPath("$.passwordNueva").value("nuevaSegura123"))
				.andRespond(withNoContent());

		boolean resultado = controller.cambiarPassword(
				Map.of("passwordActual", "vieja123", "passwordNueva", "nuevaSegura123"),
				"Bearer jwt-123");

		server.verify();
		assertThat(resultado).isTrue();
	}
}
