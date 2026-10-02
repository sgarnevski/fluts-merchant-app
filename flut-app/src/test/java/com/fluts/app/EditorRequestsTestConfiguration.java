package com.fluts.app;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fluts.app.security.Roles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Every MockMvc request comes from the Dutch merchant's editor with a valid CSRF token (security is tested separately). */
@TestConfiguration(proxyBeanMethods = false)
@Import({PostgresTestConfiguration.class, SecurityTestConfiguration.class})
public class EditorRequestsTestConfiguration {

    @Bean
    MockMvcBuilderCustomizer editorRequests() {
        return builder -> builder.defaultRequest(get("/")
                .with(jwt().jwt(token -> token.claim("preferred_username", "dutch").claim("merchant", "dutch"))
                        .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.VIEWER), new SimpleGrantedAuthority("ROLE_" + Roles.EDITOR)))
                .with(csrf()));
    }
}
