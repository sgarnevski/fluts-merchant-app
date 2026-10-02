package com.fluts.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * OIDC RP-initiated logout for a single-page app: the UI logs out with {@code fetch}, which cannot
 * follow a redirect to Keycloak. So instead of redirecting, this answers
 * {@code {"redirectUrl": "<Keycloak end-session URL>"}} and the UI navigates there; Keycloak ends
 * its session too and sends the browser back to the app.
 */
public final class JsonLogoutSuccessHandler extends OidcClientInitiatedLogoutSuccessHandler {

    public JsonLogoutSuccessHandler(final ClientRegistrationRepository clientRegistrations) {
        super(clientRegistrations);
        setPostLogoutRedirectUri("{baseUrl}/");
    }

    @Override
    public void onLogoutSuccess(final HttpServletRequest request, final HttpServletResponse response,
            final @Nullable Authentication authentication) throws IOException {
        final String redirectUrl = determineTargetUrl(request, response, authentication);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JsonMapper.shared().writeValue(response.getOutputStream(), Map.of("redirectUrl", redirectUrl));
    }
}
