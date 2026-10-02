package com.fluts.app.security;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * One filter chain for two kinds of clients:
 * <ul>
 *   <li>the browser logs in through Keycloak ({@code oauth2Login}); the server keeps the tokens in
 *       the HTTP session, the browser only gets a session cookie, and mutating requests need the
 *       CSRF token from the {@code XSRF-TOKEN} cookie;</li>
 *   <li>API clients (curl, Swagger) send {@code Authorization: Bearer <access token>} instead;
 *       those requests need no CSRF token.</li>
 * </ul>
 * Reading needs the role {@value Roles#VIEWER} (own merchant) or {@value Roles#ADMIN} (every merchant);
 * everything that changes data needs {@value Roles#EDITOR}. Which merchant's data a call sees is
 * decided in the services from the caller's merchant claim ({@link CurrentActor}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {

    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http,
            final ClientRegistrationRepository clientRegistrations) {
        final KeycloakRolesConverter rolesConverter = new KeycloakRolesConverter();
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                                "/actuator/health")
                        .permitAll()
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(Roles.VIEWER, Roles.ADMIN)
                        .requestMatchers("/api/**").hasRole(Roles.EDITOR)
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(new KeycloakAuthoritiesMapper(rolesConverter)))
                        .defaultSuccessUrl("/", true)
                        .failureHandler(new LoginFailureHandler()))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(rolesConverter))))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), PathPatternRequestMatcher.pathPattern("/api/**")))
                .csrf(csrf -> csrf.spa().ignoringRequestMatchers(SecurityConfiguration::hasBearerToken))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .logout(logout -> logout.logoutSuccessHandler(new JsonLogoutSuccessHandler(clientRegistrations)));
        return http.build();
    }

    /** Documents bearer tokens in Swagger UI; after a browser login the session cookie works too. */
    @Bean
    OpenApiCustomizer bearerSecurityScheme() {
        return openApi -> {
            openApi.addSecurityItem(new SecurityRequirement().addList("bearer"));
            openApi.getComponents().addSecuritySchemes("bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    .description("Keycloak access token; see the README for getting one with curl"));
        };
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter(final KeycloakRolesConverter rolesConverter) {
        final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> rolesConverter.convert(jwt.getClaims()));
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    private static boolean hasBearerToken(final HttpServletRequest request) {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.startsWith(BEARER_PREFIX);
    }
}
