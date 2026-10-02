package com.fluts.app.web;

import com.fluts.domain.SchuurResult;
import com.fluts.domain.Schuur;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One schuur of a stored scenario, with the best buy at this schuur.
 *
 * @param boxPrices  prices of the boxes, top to bottom
 * @param maxProfit  the profit of the best buy at this schuur
 * @param flutCounts every number of boxes from the top that earns {@code maxProfit}, ascending; {@code 0} means buy nothing
 */
public record SchuurResponse(
        @Schema(example = "[7, 3, 11, 9, 10]") List<Integer> boxPrices,
        @Schema(example = "10") long maxProfit,
        @Schema(example = "[2, 4, 5]") List<Integer> flutCounts) {

    static SchuurResponse of(final Schuur schuur, final SchuurResult result) {
        return new SchuurResponse(schuur.boxPrices(), result.maxProfit(), result.flutCounts());
    }
}
