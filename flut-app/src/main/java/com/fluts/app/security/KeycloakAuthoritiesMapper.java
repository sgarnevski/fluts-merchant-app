package com.fluts.app.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/** Adds the Keycloak realm roles of the ID token to a user who logged in through the browser. */
public final class KeycloakAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private final KeycloakRolesConverter rolesConverter;

    public KeycloakAuthoritiesMapper(final KeycloakRolesConverter rolesConverter) {
        this.rolesConverter = rolesConverter;
    }

    @Override
    public Set<GrantedAuthority> mapAuthorities(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .flatMap(authority -> authority instanceof final OidcUserAuthority oidc
                        ? Stream.concat(Stream.of(authority), rolesConverter.convert(oidc.getIdToken().getClaims()).stream())
                        : Stream.of(authority))
                .collect(Collectors.toUnmodifiableSet());
    }
}
