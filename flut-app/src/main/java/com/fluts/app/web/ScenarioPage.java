package com.fluts.app.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * One page of the scenario list.
 *
 * @param content the scenarios of this page
 * @param page    position and size of the page
 */
public record ScenarioPage(List<ScenarioSummary> content, PageInfo page) {

    /**
     * @param number        zero-based page index
     * @param size          requested page size
     * @param totalElements number of scenarios matching the filters
     * @param totalPages    number of pages
     */
    public record PageInfo(int number, int size, long totalElements, int totalPages) {
    }

    static ScenarioPage from(final Page<ScenarioSummary> page) {
        return new ScenarioPage(page.getContent(),
                new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
