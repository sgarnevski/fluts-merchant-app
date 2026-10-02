package com.fluts.app.web;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.io.InputLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Body of {@code POST /api/scenarios} and {@code PUT /api/scenarios/{id}}. */
public record ScenarioRequest(
        @Schema(example = "Example 2") @NotBlank String name,
        @NotEmpty @Size(max = InputLimits.DEFAULT_MAX_SCHUURS_PER_SCENARIO) List<@NotNull @Valid SchuurRequest> schuurs) {

    Scenario toDomain() {
        return new Scenario(name, schuurs.stream().map(schuur -> new Schuur(schuur.boxPrices())).toList());
    }
}
