package com.proyecto.servicios.config.security;

import com.proyecto.servicios.security.jwt.JwtAuthoritiesConverter;
import com.proyecto.servicios.security.jwt.JwtValidator;
import com.proyecto.servicios.security.jwt.KeycloakJwtDecoder;
import com.proyecto.servicios.security.jwt.TokenValidationException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

/**
 * Autentica la petición a partir del JWT y valida la exclusividad de sesión única y dispositivo (X-Device-Id / PPID)
 * delegando la validación formal en JwtValidator y JwtAuthoritiesConverter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtValidator jwtValidator;
    private final KeycloakJwtDecoder jwtDecoder;
    private final JwtAuthoritiesConverter authoritiesConverter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        final String headerDeviceId = request.getHeader("X-Device-Id");

        Claims claims = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            jwt = authorizationHeader.substring(BEARER_PREFIX.length());
            try {
                claims = jwtDecoder.decodeClaims(jwt);
            } catch (TokenValidationException e) {
                log.debug("JWT rechazado: {}", e.getMessage());
            }
        }

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // El token del paso intermedio del 2FA nunca autentica peticiones.
            if (JwtUtil.TYPE_PRE_AUTH.equals(claims.get(JwtUtil.CLAIM_TYPE, String.class))) {
                filterChain.doFilter(request, response);
                return;
            }

            if (claims.get(JwtUtil.CLAIM_CLIENT_ID, String.class) != null) {
                autenticarCliente(claims, request);
            } else {
                try {
                    autenticarUsuario(claims, jwt, headerDeviceId, request);
                } catch (TokenValidationException e) {
                    log.warn("[ACCESO DENEGADO JWT] Motivo: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"mensaje\":\"" + e.getMessage() + "\",\"status\":401,\"codigo\":401}");
                    return;
                } catch (Exception e) {
                    log.debug("No fue posible autenticar con el JWT: {}", e.getMessage());
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /** Token de aplicación (client_credentials): solo aporta los scopes firmados en el token. */
    private void autenticarCliente(Claims claims, HttpServletRequest request) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(claims);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** Token de usuario: valida sesión única y dispositivo contra la base de datos. */
    private void autenticarUsuario(Claims claims, String jwt, String headerDeviceId, HttpServletRequest request) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());
        if (jwtValidator.validate(jwt, userDetails, headerDeviceId)) {
            Collection<GrantedAuthority> authorities = authoritiesConverter.convert(claims);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, authorities.isEmpty() ? userDetails.getAuthorities() : authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
