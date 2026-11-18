package com.proyecto.servicios.model.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta estándar RFC 6749 (access_token, token_type, expires_in, scope). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private Long expiresIn;
    private String scope;
}
