package com.fluts.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fluts.app.PostgresTestConfiguration;
import com.fluts.app.SecurityTestConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * The single-page app gets its CSRF token as a readable {@code XSRF-TOKEN} cookie on any request.
 * Checked over real HTTP: MockMvc's security support replaces the CSRF token repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, SecurityTestConfiguration.class})
class CsrfCookieTest {

    private final int port;

    CsrfCookieTest(@LocalServerPort final int port) {
        this.port = port;
    }

    @Test
    void everyResponseOffersTheCsrfTokenAsCookie() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<Void> response = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/me")).build(),
                    HttpResponse.BodyHandlers.discarding());

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().allValues("Set-Cookie"))
                    .anySatisfy(cookie -> assertThat(cookie).startsWith("XSRF-TOKEN=").doesNotContain("HttpOnly"));
        }
    }
}
