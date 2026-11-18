package com.proyecto.servicios.model.security;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTokenRequest {
    @NotBlank(message = "El clientId es obligatorio")
    private String clientId;

    @NotBlank(message = "El clientSecret es obligatorio")
    private String clientSecret;

    @Builder.Default
    private String grantType = "client_credentials";

    @Builder.Default
    private String scope = "READ";
}
