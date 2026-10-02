package com.fluts.app.scenario;

import com.fluts.domain.Scenario;
import com.fluts.domain.SchuurResult;
import com.fluts.domain.TradingResult;
import com.fluts.persistence.ScenarioInputType;
import java.time.Instant;
import java.util.List;

/**
 * A stored scenario together with its result, computed when it was read.
 *
 * @param merchantCode  the merchant the scenario belongs to
 * @param schuurResults the best buy at each schuur, in schuur order (what to buy where)
 */
public record ScenarioView(long id, String merchantCode, Scenario scenario, ScenarioInputType inputType, Instant createdAt, Instant updatedAt,
        TradingResult result, List<SchuurResult> schuurResults) {
}
