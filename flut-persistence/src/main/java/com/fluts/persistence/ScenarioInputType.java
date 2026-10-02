package com.fluts.persistence;

import com.fluts.io.InputType;

/**
 * How a stored scenario arrived: the core's input types (JSON body, uploaded file) plus the
 * platform's own channel, a Kafka message.
 */
public enum ScenarioInputType {
    JSON,
    FILE,
    KAFKA;

    /** The stored type for scenarios that came in through the core's evaluation API. */
    public static ScenarioInputType of(final InputType inputType) {
        return valueOf(inputType.name());
    }
}
