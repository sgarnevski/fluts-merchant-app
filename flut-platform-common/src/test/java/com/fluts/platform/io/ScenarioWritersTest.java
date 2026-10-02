package com.fluts.platform.io;

import static org.assertj.core.api.Assertions.assertThat;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.io.TextScenarioParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioWritersTest {

    private static final List<Scenario> EXAMPLES = List.of(
            new Scenario("Example 1", List.of(new Schuur(List.of(12, 3, 10, 7, 16, 5)))),
            new Scenario("Example 2", List.of(
                    new Schuur(List.of(7, 3, 11, 9, 10)),
                    new Schuur(List.of()),
                    new Schuur(List.of(1, 2, 3, 4, 10, 16, 10, 4, 16)))));

    private final TextScenarioWriter textWriter = new TextScenarioWriter();
    private final JsonScenarioWriter jsonWriter = new JsonScenarioWriter();
    private final TextScenarioParser textParser = new TextScenarioParser(InputLimits.DEFAULT);
    private final JsonScenarioParser jsonParser = new JsonScenarioParser(InputLimits.DEFAULT);

    @Test
    void textWriterProducesTheSpecificationExample() throws IOException {
        try (InputStream sample = Files.newInputStream(Path.of("../demo/input.txt"))) {
            final List<Scenario> scenarios = textParser.parse(sample);

            assertThat(textWriter.write(scenarios)).isEqualTo(Files.readString(Path.of("../demo/input.txt")));
        }
    }

    @Test
    void textRoundTripKeepsEverythingButTheNames() {
        final List<Scenario> parsed = textParser.parse(stream(textWriter.write(EXAMPLES)));

        assertThat(parsed).extracting(Scenario::schuurs)
                .containsExactly(EXAMPLES.get(0).schuurs(), EXAMPLES.get(1).schuurs());
        assertThat(parsed).extracting(Scenario::name).containsExactly("Scenario 1", "Scenario 2");
    }

    @Test
    void textWriterEndsEmptyInputWithTheTerminator() {
        assertThat(textWriter.write(List.of())).isEqualTo("0\n");
    }

    @Test
    void jsonRoundTripKeepsEverything() {
        final String json = jsonWriter.write(EXAMPLES);

        assertThat(json).endsWith("}\n").contains("\n");
        assertThat(jsonParser.parse(stream(json))).isEqualTo(EXAMPLES);
    }

    @Test
    void singleScenarioRoundTripOnOneLine() {
        final String json = jsonWriter.writeScenario(EXAMPLES.get(1));

        assertThat(json).doesNotContain("\n").startsWith("{\"name\":\"Example 2\"");
        assertThat(jsonParser.parse(stream("{\"scenarios\": [" + json + "]}"))).containsExactly(EXAMPLES.get(1));
    }

    private static InputStream stream(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
