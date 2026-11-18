package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.security.JwtUtil;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.model.security.OAuthTokenRequest;
import com.proyecto.servicios.model.security.OAuthTokenResponse;
import com.proyecto.servicios.service.OAuthClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class OAuthClientServiceImpl implements OAuthClientService {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private static final String MSG_INVALIDO = "Credenciales de cliente inválidas";

    private final JwtUtil jwtUtil;
    private final String clientId;
    private final String clientSecret;
    private final List<String> scopesPermitidos;

    public OAuthClientServiceImpl(JwtUtil jwtUtil,
                                  @Value("${app.oauth2.client-id}") String clientId,
                                  @Value("${app.oauth2.client-secret}") String clientSecret,
                                  @Value("${app.oauth2.client-scopes:READ}") List<String> scopesPermitidos) {
        this.jwtUtil = jwtUtil;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopesPermitidos = scopesPermitidos.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();
    }

    @Override
    public OAuthTokenResponse emitirToken(OAuthTokenRequest request) {
        if (!GRANT_CLIENT_CREDENTIALS.equals(request.getGrantType())) {
            throw new UnauthorizedAccessException(MSG_INVALIDO);
        }
        // Comparación en tiempo constante para no filtrar información por temporización.
        boolean idOk = iguales(clientId, request.getClientId());
        boolean secretOk = iguales(clientSecret, request.getClientSecret());
        if (!(idOk && secretOk)) {
            log.warn("[OAUTH] Intento de obtener token con credenciales de cliente inválidas");
            throw new UnauthorizedAccessException(MSG_INVALIDO);
        }

        List<String> solicitados = resolverScopes(request.getScope());
        String token = jwtUtil.generateClientToken(clientId, solicitados);

        return OAuthTokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(JwtUtil.CLIENT_TOKEN_TTL_SECONDS)
                .scope(String.join(" ", solicitados))
                .build();
    }

    /** El cliente solo puede obtener scopes de su lista permitida; pedir de más se rechaza. */
    private List<String> resolverScopes(String scopeSolicitado) {
        if (scopeSolicitado == null || scopeSolicitado.isBlank()) {
            return scopesPermitidos;
        }
        List<String> solicitados = Arrays.stream(scopeSolicitado.trim().split("\\s+"))
                .map(s -> s.toUpperCase(Locale.ROOT).replaceFirst("^SCOPE_", ""))
                .toList();
        if (!scopesPermitidos.containsAll(solicitados)) {
            throw new UnauthorizedAccessException("Scope no permitido para este cliente");
        }
        return solicitados;
    }

    private boolean iguales(String esperado, String recibido) {
        if (esperado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.trim().getBytes(StandardCharsets.UTF_8));
    }
}
