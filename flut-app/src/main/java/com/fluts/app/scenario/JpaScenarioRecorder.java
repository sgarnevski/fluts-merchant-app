package com.fluts.app.scenario;

import com.fluts.app.security.CurrentActor;
import com.fluts.domain.Scenario;
import com.fluts.io.InputType;
import com.fluts.persistence.ScenarioInputType;
import com.fluts.rest.evaluation.ScenarioRecorder;
import java.util.List;
import org.springframework.stereotype.Component;

/** Saves every evaluated or imported scenario for the calling user's merchant, so all input ends up in the database. */
@Component
public class JpaScenarioRecorder implements ScenarioRecorder {

    private final ScenarioService scenarioService;
    private final CurrentActor currentActor;

    public JpaScenarioRecorder(final ScenarioService scenarioService, final CurrentActor currentActor) {
        this.scenarioService = scenarioService;
        this.currentActor = currentActor;
    }

    @Override
    public List<Long> record(final List<Scenario> scenarios, final InputType inputType) {
        return scenarioService.saveAll(currentActor.get().requireMerchant(), scenarios, ScenarioInputType.of(inputType));
    }
}
