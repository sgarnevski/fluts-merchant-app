package com.fluts.app;

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts the real app through {@code main()}. At startup the app reads Keycloak's OpenID
 * configuration, so a tiny HTTP server stands in for Keycloak here.
 */
@Testcontainers
class FlutApplicationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestConfiguration.IMAGE);

    private static final HttpServer KEYCLOAK = startKeycloakStandIn();

    @BeforeAll
    static void startKeycloak() {
        KEYCLOAK.start();
    }

    @AfterAll
    static void stopKeycloak() {
        KEYCLOAK.stop(0);
    }

    @Test
    void mainStartsTheApplication() {
        assertThatNoException().isThrownBy(() -> FlutApplication.main(new String[] {
            "--spring.main.web-application-type=none",
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--flut.keycloak.issuer-uri=" + issuer()}));
    }

    private static String issuer() {
        return "http://localhost:" + KEYCLOAK.getAddress().getPort() + "/realms/flut";
    }

    private static HttpServer startKeycloakStandIn() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/realms/flut/.well-known/openid-configuration", exchange -> {
                final String issuer = issuer();
                final byte[] body = """
                        {"issuer": "%1$s", "authorization_endpoint": "%1$s/auth", "token_endpoint": "%1$s/token",
                         "jwks_uri": "%1$s/certs", "userinfo_endpoint": "%1$s/userinfo",
                         "end_session_endpoint": "%1$s/logout", "subject_types_supported": ["public"],
                         "response_types_supported": ["code"]}""".formatted(issuer).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            return server;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
