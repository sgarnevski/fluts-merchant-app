package com.fluts.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fluts.app.EditorRequestsTestConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API contract: the live OpenAPI document must equal the committed {@code api/flut-app.openapi.json},
 * from which the frontend generates its TypeScript types. After an intended API change run
 * {@code ./mvnw -pl flut-app verify -Dopenapi.update=true} and commit the updated file.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(EditorRequestsTestConfiguration.class)
class OpenApiContractTest {

    private static final Path CONTRACT = Path.of("../api/flut-app.openapi.json");
    private static final JsonMapper MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private final MockMvcTester mvc;

    OpenApiContractTest(@Autowired final MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void liveApiMatchesTheCommittedContract() throws IOException {
        final String live = mvc.get().uri("/v3/api-docs").exchange().getResponse().getContentAsString(StandardCharsets.UTF_8);
        final JsonNode liveContract = MAPPER.readTree(live);

        if (Boolean.getBoolean("openapi.update")) {
            Files.writeString(CONTRACT, MAPPER.writeValueAsString(liveContract) + "\n");
        }

        assertThat(liveContract)
                .as("API contract changed - rerun with -Dopenapi.update=true and commit %s", CONTRACT)
                .isEqualTo(MAPPER.readTree(Files.readString(CONTRACT)));
    }
}
