package com.fluts.platform.io;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.io.TextScenarioParser;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes scenarios in the original text format of the specification (the counterpart of
 * {@link TextScenarioParser}), including the terminating {@code 0}. The format has no scenario names.
 */
public final class TextScenarioWriter {

    private static final String TERMINATOR = "0\n";

    public String write(final List<Scenario> scenarios) {
        return scenarios.stream().map(TextScenarioWriter::writeScenario).collect(Collectors.joining()) + TERMINATOR;
    }

    private static String writeScenario(final Scenario scenario) {
        return scenario.schuurs().size() + "\n"
                + scenario.schuurs().stream().map(TextScenarioWriter::writeSchuur).collect(Collectors.joining());
    }

    private static String writeSchuur(final Schuur schuur) {
        return Stream.concat(Stream.of(schuur.boxPrices().size()), schuur.boxPrices().stream())
                .map(String::valueOf)
                .collect(Collectors.joining(" ", "", "\n"));
    }
}
