package com.sisagrodev.auth.web.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

	public static TokenResponse bearer(String token, long expiresIn) {
		return new TokenResponse(token, "Bearer", expiresIn);
	}
}
