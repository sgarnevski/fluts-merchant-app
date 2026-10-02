package com.fluts.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import com.fluts.app.PostgresTestConfiguration;
import com.fluts.app.SecurityTestConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Who may do what: browser sessions (OIDC login + CSRF) and bearer tokens, against the real filter chain. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, SecurityTestConfiguration.class})
class SecurityTest {

    private static final String SCENARIO = "{\"name\": \"Secured\", \"schuurs\": [{\"boxPrices\": [1]}]}";

    private final MockMvcTester mvc;
    private final ClientRegistrationRepository clientRegistrations;

    SecurityTest(@Autowired final MockMvcTester mvc, @Autowired final ClientRegistrationRepository clientRegistrations) {
        this.mvc = mvc;
        this.clientRegistrations = clientRegistrations;
    }

    @Test
    void anonymousApiCallsAreUnauthorizedNotRedirected() {
        assertThat(mvc.get().uri("/api/scenarios")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymousBrowsersAreSentToTheKeycloakLogin() {
        assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML))
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl().endsWith("/oauth2/authorization/keycloak");
    }

    @Test
    void aLoginCallbackWithoutTheLoginThatStartedItStartsTheLoginAgain() {
        assertThat(mvc.get().uri("/login/oauth2/code/keycloak?code=abc&state=unknown"))
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl().isEqualTo("/oauth2/authorization/keycloak");
    }

    @Test
    void apiDocumentationAndHealthArePublic() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
    }

    @Test
    void viewersMayReadButNotWrite() {
        assertThat(mvc.get().uri("/api/scenarios").with(login("viewer", "dutch", Roles.VIEWER))).hasStatusOk();
        assertThat(mvc.post().uri("/api/scenarios").with(login("viewer", "dutch", Roles.VIEWER)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void usersWithoutAppRolesMayNotRead() {
        assertThat(mvc.get().uri("/api/scenarios").with(login("nobody", "dutch"))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void browserWritesNeedTheCsrfToken() {
        assertThat(mvc.post().uri("/api/scenarios").with(login("dutch", "dutch", Roles.VIEWER, Roles.EDITOR))
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/api/scenarios").with(login("dutch", "dutch", Roles.VIEWER, Roles.EDITOR)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.merchant").isEqualTo("dutch");
    }

    @Test
    void merchantsSeeOnlyTheirOwnScenarios() {
        final long dutchId = create(SecurityTestConfiguration.DUTCH_TOKEN, "Dutch only");
        final long frenchId = create(SecurityTestConfiguration.FRENCH_TOKEN, "French only");

        assertThat(mvc.get().uri("/api/scenarios/{id}", frenchId).header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN)))
                .as("another merchant's scenario looks like a missing one").hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/scenarios/{id}", dutchId).header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN)))
                .hasStatusOk().bodyJson().extractingPath("$.merchant").isEqualTo("dutch");
        assertThat(mvc.put().uri("/api/scenarios/{id}", frenchId).header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN))
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/scenarios/{id}", frenchId).header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/scenarios").param("name", "only").param("merchant", "french")
                .header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN)))
                .as("the merchant filter cannot widen a merchant user's view")
                .bodyJson().extractingPath("$.content[*].merchant").asArray().containsOnly("dutch");
    }

    @Test
    void adminsSeeEveryMerchantButChangeNothing() {
        final long dutchId = create(SecurityTestConfiguration.DUTCH_TOKEN, "Admin sees dutch");
        create(SecurityTestConfiguration.FRENCH_TOKEN, "Admin sees french");

        assertThat(mvc.get().uri("/api/scenarios/{id}", dutchId).header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.ADMIN_TOKEN)))
                .hasStatusOk().bodyJson().extractingPath("$.merchant").isEqualTo("dutch");

        assertThat(mvc.get().uri("/api/scenarios").param("name", "Admin sees")
                .header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.ADMIN_TOKEN)))
                .hasStatusOk().bodyJson().extractingPath("$.content[*].merchant").asArray().containsExactlyInAnyOrder("dutch", "french");
        assertThat(mvc.get().uri("/api/scenarios").param("name", "Admin sees").param("merchant", "french")
                .header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.ADMIN_TOKEN)))
                .bodyJson().extractingPath("$.content[*].merchant").asArray().containsExactly("french");
        assertThat(mvc.post().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.ADMIN_TOKEN))
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void editorsWithoutAMerchantCannotCreate() {
        assertThat(mvc.post().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.HOMELESS_TOKEN))
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.detail").isEqualTo("User 'homeless' is not assigned to a merchant");
        assertThat(mvc.get().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.HOMELESS_TOKEN)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void bearerTokensWorkWithoutCsrfToken() {
        assertThat(mvc.post().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN))
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.CREATED);
        assertThat(mvc.post().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.VIEWER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void invalidBearerTokensAreUnauthorized() {
        assertThat(mvc.get().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer("forged")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void otherAuthorizationSchemesStillNeedTheCsrfToken() {
        assertThat(mvc.post().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, "Basic ZWRpdG9yOmVkaXRvcg==")
                .contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void meShowsTheUserTheirAppRolesAndTheirMerchant() {
        assertThat(mvc.get().uri("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.DUTCH_TOKEN)))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"username": "dutch", "roles": ["editor", "viewer"], "merchant": {"code": "dutch", "name": "Dutch merchant"}}""");
        assertThat(mvc.get().uri("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(SecurityTestConfiguration.ADMIN_TOKEN)))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("{\"username\": \"admin\", \"roles\": [\"admin\"], \"merchant\": null}");
        assertThat(mvc.get().uri("/api/me").with(login("nobody", "unregistered")))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"username": "nobody", "roles": [], "merchant": {"code": "unregistered", "name": "unregistered"}}""");
    }

    @Test
    void logoutAnswersWithTheKeycloakEndSessionUrl() {
        assertThat(mvc.post().uri("/logout").with(login("dutch", "dutch", Roles.EDITOR)).with(csrf()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.redirectUrl").asString()
                .startsWith(SecurityTestConfiguration.END_SESSION_URI + "?id_token_hint=")
                .contains("post_logout_redirect_uri=http://localhost/");
    }

    /** A browser session as after a Keycloak login; the user name comes from {@code preferred_username}, as configured. */
    private OidcLoginRequestPostProcessor login(final String username, final String merchant, final String... roles) {
        final List<GrantedAuthority> authorities = Stream.of(roles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        final OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
                .subject("id-of-" + username)
                .claim("preferred_username", username)
                .claim("merchant", merchant)
                .build();
        return oidcLogin()
                .clientRegistration(clientRegistrations.findByRegistrationId("keycloak"))
                .oidcUser(new DefaultOidcUser(authorities, idToken, "preferred_username"));
    }

    private long create(final String token, final String name) {
        final byte[] body = mvc.post().uri("/api/scenarios").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\", \"schuurs\": [{\"boxPrices\": [1]}]}")
                .exchange().getResponse().getContentAsByteArray();
        return JsonPath.<Number>read(new String(body, StandardCharsets.UTF_8), "$.id").longValue();
    }

    private static String bearer(final String token) {
        return "Bearer " + token;
    }
}
