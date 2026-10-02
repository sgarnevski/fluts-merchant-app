package com.fluts.app.web;

import com.fluts.app.scenario.ScenarioView;
import com.fluts.persistence.ScenarioInputType;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

/** A stored scenario with its result (computed when read); {@code merchant} is the code of the merchant it belongs to. */
public record ScenarioResponse(long id, String merchant, String name, ScenarioInputType inputType, List<SchuurResponse> schuurs,
        long maxProfit, List<Integer> flutCounts, Instant createdAt, Instant updatedAt) {

    static ScenarioResponse from(final ScenarioView view) {
        return new ScenarioResponse(view.id(), view.merchantCode(), view.scenario().name(), view.inputType(),
                IntStream.range(0, view.schuurResults().size())
                        .mapToObj(index -> SchuurResponse.of(view.scenario().schuurs().get(index), view.schuurResults().get(index)))
                        .toList(),
                view.result().maxProfit(), view.result().flutCounts(), view.createdAt(), view.updatedAt());
    }
}
