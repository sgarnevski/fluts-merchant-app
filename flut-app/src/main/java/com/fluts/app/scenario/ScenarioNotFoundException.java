package com.fluts.app.scenario;

import java.io.Serial;

/** No scenario with the requested id (or, for a subclass, no such part of it). */
public class ScenarioNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ScenarioNotFoundException(final long id) {
        this("Scenario " + id + " not found");
    }

    protected ScenarioNotFoundException(final String message) {
        super(message);
    }

    /** The scenario exists but has no schuur at the requested position. */
    public static final class SchuurNotFoundException extends ScenarioNotFoundException {

        @Serial
        private static final long serialVersionUID = 1L;

        public SchuurNotFoundException(final long scenarioId, final int position) {
            super("Scenario " + scenarioId + " has no schuur " + position);
        }
    }
}
