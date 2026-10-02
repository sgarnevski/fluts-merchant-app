package com.fluts.app.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * The caller of the current request, from the security context. The merchant is the token claim
 * {@code merchant} (a Keycloak user attribute), for browser logins and bearer tokens alike — never
 * anything the request body says.
 */
@Component
public class CurrentActor {

    static final String MERCHANT_CLAIM = "merchant";

    public Actor get() {
        return of(SecurityContextHolder.getContext().getAuthentication());
    }

    public Actor of(final Authentication authentication) {
        final boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + Roles.ADMIN)::equals);
        return new Actor(authentication.getName(), merchantOf(authentication.getPrincipal()), admin);
    }

    private static @Nullable String merchantOf(final Object principal) {
        return switch (principal) {
            case final OidcUser user -> user.getClaimAsString(MERCHANT_CLAIM);
            case final Jwt jwt -> jwt.getClaimAsString(MERCHANT_CLAIM);
            default -> null;
        };
    }
}
