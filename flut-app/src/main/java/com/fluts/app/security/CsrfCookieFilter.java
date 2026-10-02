package com.fluts.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security creates the CSRF token lazily, so a single-page app that has only made GET requests
 * would never receive the {@code XSRF-TOKEN} cookie. Loading the token on every request writes it.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        Objects.requireNonNull((CsrfToken) request.getAttribute(CsrfToken.class.getName()), "runs after the CsrfFilter")
                .getToken();
        filterChain.doFilter(request, response);
    }
}
