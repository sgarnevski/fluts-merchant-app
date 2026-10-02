package com.fluts.app.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * What happens when a browser login through Keycloak cannot be completed.
 *
 * <p>The common case is harmless: the app no longer has the session that started the login (it was
 * restarted, or the login page stayed open longer than the session lives), so Keycloak's answer
 * cannot be matched ({@value #REQUEST_NOT_FOUND}). The user did log in at Keycloak, so the login is
 * simply started again — Keycloak's session makes that invisible. A short-lived cookie allows this
 * once, so a browser that drops cookies cannot loop.
 *
 * <p>Every other failure goes to the UI's {@value #FAILURE_PAGE} page instead of Spring's default
 * login page, which would misleadingly say "Invalid credentials".
 */
public final class LoginFailureHandler implements AuthenticationFailureHandler {

    static final String REQUEST_NOT_FOUND = "authorization_request_not_found";
    static final String RETRY_COOKIE = "flut_login_retry";
    static final String LOGIN_PATH = "/oauth2/authorization/keycloak";
    static final String FAILURE_PAGE = "/sign-in-failed";

    private static final Logger LOG = LoggerFactory.getLogger(LoginFailureHandler.class);
    private static final int RETRY_WINDOW_SECONDS = 60;

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException exception) throws IOException {
        if (isRequestNotFound(exception) && !retriedRecently(request)) {
            LOG.info("Login could not be matched to a session (app restarted or session expired); starting it again");
            final Cookie retry = new Cookie(RETRY_COOKIE, "1");
            retry.setPath("/");
            retry.setHttpOnly(true);
            retry.setMaxAge(RETRY_WINDOW_SECONDS);
            response.addCookie(retry);
            response.sendRedirect(request.getContextPath() + LOGIN_PATH);
            return;
        }
        LOG.warn("Login failed: {}", exception.getMessage());
        response.sendRedirect(request.getContextPath() + FAILURE_PAGE);
    }

    private static boolean isRequestNotFound(final AuthenticationException exception) {
        return exception instanceof final OAuth2AuthenticationException oauth
                && REQUEST_NOT_FOUND.equals(oauth.getError().getErrorCode());
    }

    private static boolean retriedRecently(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        return cookies != null && Arrays.stream(cookies).anyMatch(cookie -> RETRY_COOKIE.equals(cookie.getName()));
    }
}
