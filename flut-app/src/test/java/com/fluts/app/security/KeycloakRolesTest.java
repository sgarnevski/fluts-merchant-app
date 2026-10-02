package com.fluts.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

class KeycloakRolesTest {

    private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

    @Test
    void mapsRealmRolesToAuthorities() {
        assertThat(converter.convert(Map.of("realm_access", Map.of("roles", List.of("viewer", "editor")))))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_viewer", "ROLE_editor");
    }

    @Test
    void ignoresMissingOrMalformedClaims() {
        assertThat(converter.convert(Map.of())).isEmpty();
        assertThat(converter.convert(Map.of("realm_access", "not an object"))).isEmpty();
        assertThat(converter.convert(Map.of("realm_access", Map.of("roles", "not a list")))).isEmpty();
    }

    @Test
    void addsTheRolesOfTheIdTokenToABrowserLogin() {
        final OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "user-1", "realm_access", Map.of("roles", List.of("editor"))));
        final GrantedAuthority oidcAuthority = new OidcUserAuthority(idToken);
        final GrantedAuthority scope = new SimpleGrantedAuthority("SCOPE_openid");

        assertThat(new KeycloakAuthoritiesMapper(converter).mapAuthorities(List.of(oidcAuthority, scope)))
                .containsExactlyInAnyOrder(oidcAuthority, scope, new SimpleGrantedAuthority("ROLE_editor"));
    }
}
