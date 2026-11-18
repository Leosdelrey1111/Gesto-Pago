package com.proyecto.servicios.service;

import com.proyecto.servicios.model.security.OAuthTokenRequest;
import com.proyecto.servicios.model.security.OAuthTokenResponse;

public interface OAuthClientService {

    /** Flujo client_credentials: valida client_id/client_secret y emite un token limitado a los scopes permitidos. */
    OAuthTokenResponse emitirToken(OAuthTokenRequest request);
}
