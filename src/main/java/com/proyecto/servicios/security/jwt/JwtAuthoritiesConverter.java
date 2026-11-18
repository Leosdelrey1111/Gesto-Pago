package com.proyecto.servicios.security.jwt;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convierte los claims de roles tanto de Keycloak (realm_access.roles)
 * como de tokens internos a una colección de GrantedAuthority de Spring Security.
 */
@Slf4j
@Component
public class JwtAuthoritiesConverter implements Converter<Claims, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String SCOPE_CLAIM = "scope";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Claims claims) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (claims == null) {
            return authorities;
        }

        // 1. Roles estándar directos en claim "roles" o "authorities"
        Object rolesObj = claims.get(ROLES_CLAIM);
        if (rolesObj instanceof List<?> list) {
            list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(this::formatearRol)
                    .forEach(authorities::add);
        }

        // 2. Scopes estándar OAuth2 (SCOPE_READ, SCOPE_WRITE)
        Object scopeObj = claims.get(SCOPE_CLAIM);
        if (scopeObj instanceof List<?> list) {
            list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(s -> s.startsWith("SCOPE_") ? s : "SCOPE_" + s.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        } else if (scopeObj instanceof String scopeStr) {
            Arrays.stream(scopeStr.split(" "))
                    .filter(s -> !s.isBlank())
                    .map(s -> s.startsWith("SCOPE_") ? s : "SCOPE_" + s.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // 2. Roles anidados de Keycloak en claim "realm_access.roles"
        Object realmAccessObj = claims.get(REALM_ACCESS_CLAIM);
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object realmRoles = realmAccess.get(ROLES_CLAIM);
            if (realmRoles instanceof List<?> list) {
                list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(this::formatearRol)
                        .forEach(authorities::add);
            }
        }

        // Mínimo privilegio: sin roles ni scopes en el token no se concede ninguna autoridad.
        return authorities;
    }

    private GrantedAuthority formatearRol(String rol) {
        String rolLimpio = rol.toUpperCase().trim();
        if (!rolLimpio.startsWith(ROLE_PREFIX)) {
            rolLimpio = ROLE_PREFIX + rolLimpio;
        }
        return new SimpleGrantedAuthority(rolLimpio);
    }
}

