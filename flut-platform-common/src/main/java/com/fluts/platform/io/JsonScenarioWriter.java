package com.fluts.platform.io;

import com.fluts.domain.Scenario;
import com.fluts.io.JsonScenarioParser;
import com.fluts.io.ScenarioInput;
import com.fluts.io.ScenariosInput;
import com.fluts.io.SchuurInput;
import java.util.List;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Writes scenarios in the JSON format (the counterpart of {@link JsonScenarioParser}). */
public final class JsonScenarioWriter {

    private static final JsonMapper COMPACT = JsonMapper.builder().build();
    private static final JsonMapper PRETTY = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    /** A document {@code {"scenarios": [...]}}, indented, ending with a newline. */
    public String write(final List<Scenario> scenarios) {
        return PRETTY.writeValueAsString(new ScenariosInput(scenarios.stream().map(JsonScenarioWriter::toInput).toList()))
                + "\n";
    }

    /** A single scenario {@code {"name": ..., "schuurs": [...]}} on one line (e.g. one Kafka message). */
    public String writeScenario(final Scenario scenario) {
        return COMPACT.writeValueAsString(toInput(scenario));
    }

    private static ScenarioInput toInput(final Scenario scenario) {
        return new ScenarioInput(scenario.name(),
                scenario.schuurs().stream().map(schuur -> new SchuurInput(schuur.boxPrices())).toList());
    }
}
