package com.fluts.app;

import com.fluts.app.security.Roles;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Keycloak stand-ins, so tests need no Keycloak: a client registration with fixed endpoints (no
 * discovery at startup) and a JWT decoder that knows two tokens, {@value #EDITOR_TOKEN} and
 * {@value #VIEWER_TOKEN}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SecurityTestConfiguration {

    /** A Dutch editor. */
    public static final String DUTCH_TOKEN = "dutch-token";
    /** A French editor. */
    public static final String FRENCH_TOKEN = "french-token";
    /** A Dutch viewer. */
    public static final String VIEWER_TOKEN = "viewer-token";
    /** An admin: every merchant, read-only. */
    public static final String ADMIN_TOKEN = "admin-token";
    /** An editor without a merchant. */
    public static final String HOMELESS_TOKEN = "homeless-token";
    public static final String END_SESSION_URI = "http://keycloak.test/realms/flut/protocol/openid-connect/logout";

    private static final String REALM = "http://keycloak.test/realms/flut";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("keycloak")
                .clientId("flut-app")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile")
                .issuerUri(REALM)
                .authorizationUri(REALM + "/protocol/openid-connect/auth")
                .tokenUri(REALM + "/protocol/openid-connect/token")
                .jwkSetUri(REALM + "/protocol/openid-connect/certs")
                .userInfoUri(REALM + "/protocol/openid-connect/userinfo")
                .userNameAttributeName("preferred_username")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", END_SESSION_URI))
                .build());
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> switch (token) {
            case DUTCH_TOKEN -> jwt(token, "dutch", "dutch", List.of(Roles.VIEWER, Roles.EDITOR, "offline_access"));
            case FRENCH_TOKEN -> jwt(token, "french", "french", List.of(Roles.VIEWER, Roles.EDITOR));
            case VIEWER_TOKEN -> jwt(token, "viewer", "dutch", List.of(Roles.VIEWER));
            case ADMIN_TOKEN -> jwt(token, "admin", null, List.of(Roles.ADMIN));
            case HOMELESS_TOKEN -> jwt(token, "homeless", null, List.of(Roles.VIEWER, Roles.EDITOR));
            default -> throw new BadJwtException("Unknown token");
        };
    }

    private static Jwt jwt(final String token, final String username, final @Nullable String merchant,
            final List<String> roles) {
        final Jwt.Builder builder = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer(REALM)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", roles));
        if (merchant != null) {
            builder.claim("merchant", merchant);
        }
        return builder.build();
    }
}
