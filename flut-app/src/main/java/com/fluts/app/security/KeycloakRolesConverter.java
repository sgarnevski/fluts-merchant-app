package com.fluts.app.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Keycloak puts realm roles into the claim {@code realm_access.roles}; Spring expects authorities
 * {@code ROLE_*}. One mapping, used for both the login ID token and bearer access tokens.
 */
public final class KeycloakRolesConverter implements Converter<Map<String, Object>, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(final Map<String, Object> claims) {
        if (claims.get("realm_access") instanceof final Map<?, ?> realmAccess
                && realmAccess.get("roles") instanceof final Collection<?> roles) {
            return roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        }
        return List.of();
    }
}
