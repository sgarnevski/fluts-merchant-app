package com.fluts.app.web;

import com.fluts.io.InputLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * One schuur of a scenario request.
 *
 * @param boxPrices prices of the boxes, top to bottom; may be empty
 */
public record SchuurRequest(
        @Schema(example = "[7, 3, 11, 9, 10]")
        @NotNull @Size(max = InputLimits.DEFAULT_MAX_BOXES_PER_SCHUUR) List<@NotNull @Positive Integer> boxPrices) {
}
