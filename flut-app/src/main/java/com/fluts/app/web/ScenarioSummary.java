package com.fluts.app.web;

import com.fluts.app.scenario.ScenarioView;
import com.fluts.persistence.ScenarioInputType;
import java.time.Instant;

/** One row of the scenario list. */
public record ScenarioSummary(long id, String merchant, String name, ScenarioInputType inputType, int schuurCount, long maxProfit,
        Instant createdAt, Instant updatedAt) {

    static ScenarioSummary from(final ScenarioView view) {
        return new ScenarioSummary(view.id(), view.merchantCode(), view.scenario().name(), view.inputType(),
                view.scenario().schuurs().size(), view.result().maxProfit(), view.createdAt(), view.updatedAt());
    }
}
