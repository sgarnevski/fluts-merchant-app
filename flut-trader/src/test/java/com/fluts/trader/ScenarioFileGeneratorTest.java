package com.fluts.trader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fluts.domain.Scenario;
import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.io.ScenarioParseException;
import com.fluts.io.TextScenarioParser;
import com.fluts.platform.signing.HmacSigner;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ScenarioFileGeneratorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-30T10:15:30.123Z"), ZoneOffset.UTC);
    private static final HmacSigner SIGNER = new HmacSigner("dutch-secret");

    @Test
    void generatesValidFilesInBothFormatsWithinTheLimits() {
        final ScenarioFileGenerator generator = new ScenarioFileGenerator(new Random(42), CLOCK, "dutch", SIGNER, 0);

        final List<GeneratedFile> files = IntStream.rangeClosed(1, 200).mapToObj(generator::generate).toList();

        assertThat(files).extracting(file -> file.name().substring(file.name().lastIndexOf('.')))
                .contains(".txt", ".json").containsOnly(".txt", ".json");
        final List<Scenario> scenarios = files.stream().flatMap(file -> parse(file).stream()).toList();
        assertThat(files).allSatisfy(file -> assertThat(parse(file)).hasSizeBetween(1, ScenarioFileGenerator.MAX_SCENARIOS));
        assertThat(scenarios).allSatisfy(scenario -> {
            assertThat(scenario.schuurs()).hasSizeBetween(1, ScenarioFileGenerator.MAX_SCHUURS);
            assertThat(scenario.schuurs()).allSatisfy(schuur -> {
                assertThat(schuur.boxPrices()).hasSizeLessThanOrEqualTo(ScenarioFileGenerator.MAX_BOXES);
                assertThat(schuur.boxPrices()).allMatch(price -> price >= 1 && price <= ScenarioFileGenerator.MAX_PRICE);
            });
        });
        assertThat(scenarios).flatExtracting(Scenario::schuurs).anyMatch(schuur -> schuur.boxPrices().isEmpty());
    }

    @Test
    void namesFilesByTimeAndSequenceAndJsonScenariosReadably() {
        final ScenarioFileGenerator generator = new ScenarioFileGenerator(new Random(7), CLOCK, "french", SIGNER, 0);

        final List<GeneratedFile> files = IntStream.rangeClosed(1, 20).mapToObj(generator::generate).toList();

        assertThat(files).extracting(GeneratedFile::name).doesNotHaveDuplicates()
                .allMatch(name -> name.matches("french-20260930-101530123-\\d+\\.(txt|json)"));
        final GeneratedFile json = files.stream().filter(file -> file.name().endsWith(".json")).findFirst().orElseThrow();
        assertThat(parse(json).getFirst().name()).matches("French delivery 2026-09-30 10:15:30 #\\d+\\.1");
    }

    @Test
    void signsEveryFileWithTheMerchantsSecret() {
        final ScenarioFileGenerator generator = new ScenarioFileGenerator(new Random(5), CLOCK, "dutch", SIGNER, 0.5);

        final List<GeneratedFile> files = IntStream.rangeClosed(1, 20).mapToObj(generator::generate).toList();

        assertThat(files).allSatisfy(file -> {
            assertThat(SIGNER.verify(file.content(), file.signature())).isTrue();
            assertThat(file.signatureName()).isEqualTo(file.name() + ".sig");
        });
    }

    @Test
    void generatesBrokenFilesOnRequest() {
        final ScenarioFileGenerator generator = new ScenarioFileGenerator(new Random(1), CLOCK, "dutch", SIGNER, 1);

        final GeneratedFile file = generator.generate(1);

        assertThat(file.name()).isEqualTo("dutch-20260930-101530123-1.txt");
        assertThatThrownBy(() -> parse(file)).isInstanceOf(ScenarioParseException.class);
    }

    private static List<Scenario> parse(final GeneratedFile file) {
        final ByteArrayInputStream content = new ByteArrayInputStream(file.content().getBytes(StandardCharsets.UTF_8));
        return file.name().endsWith(".json")
                ? new JsonScenarioParser(InputLimits.DEFAULT).parse(content)
                : new TextScenarioParser(InputLimits.DEFAULT).parse(content);
    }
}
