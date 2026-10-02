package com.fluts.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class LoginFailureHandlerTest {

    private final LoginFailureHandler handler = new LoginFailureHandler();

    @Test
    void startsTheLoginAgainWhenTheAppNoLongerKnowsTheLoginItStarted() throws IOException {
        final MockHttpServletResponse response = fail(new MockHttpServletRequest(), requestNotFound());

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/authorization/keycloak");
        assertThat(response.getCookie("flut_login_retry")).isNotNull()
                .satisfies(cookie -> assertThat(cookie.getMaxAge()).isEqualTo(60));
    }

    @Test
    void ignoresUnrelatedCookiesWhenDecidingToRetry() throws IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("JSESSIONID", "x"));

        assertThat(fail(request, requestNotFound()).getRedirectedUrl()).isEqualTo("/oauth2/authorization/keycloak");
    }

    @Test
    void doesNotRetryTwiceInARowSoABrowserWithoutCookiesCannotLoop() throws IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"), new Cookie("flut_login_retry", "1"));

        assertThat(fail(request, requestNotFound()).getRedirectedUrl()).isEqualTo("/sign-in-failed");
    }

    @Test
    void sendsOtherOAuthFailuresToTheSignInFailedPage() throws IOException {
        final OAuth2AuthenticationException denied = new OAuth2AuthenticationException(new OAuth2Error("access_denied"));

        assertThat(fail(new MockHttpServletRequest(), denied).getRedirectedUrl()).isEqualTo("/sign-in-failed");
    }

    @Test
    void sendsNonOAuthFailuresToTheSignInFailedPage() throws IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"));

        assertThat(fail(request, new BadCredentialsException("no")).getRedirectedUrl()).isEqualTo("/sign-in-failed");
    }

    private MockHttpServletResponse fail(final MockHttpServletRequest request,
            final AuthenticationException exception) throws IOException {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(request, response, exception);
        return response;
    }

    private static OAuth2AuthenticationException requestNotFound() {
        return new OAuth2AuthenticationException(new OAuth2Error("authorization_request_not_found"));
    }
}
