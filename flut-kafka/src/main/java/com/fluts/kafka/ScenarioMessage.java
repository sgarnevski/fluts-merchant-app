package com.fluts.kafka;

import com.fluts.domain.Scenario;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * One scenario on its way through Kafka.
 *
 * @param messageId    identifies the message; a consumer stores it to recognise a message delivered twice
 * @param merchantCode the merchant the scenario belongs to
 * @param scenario     the scenario
 */
public record ScenarioMessage(UUID messageId, String merchantCode, Scenario scenario) {

    /**
     * The messages for the scenarios of one file. The ids are name-based UUIDs of the file content and
     * the scenario's position, so sending the same file again produces the same ids.
     */
    public static List<ScenarioMessage> forFile(final String merchantCode, final byte[] fileContent,
            final List<Scenario> scenarios) {
        final UUID fileId = UUID.nameUUIDFromBytes(fileContent);
        return IntStream.range(0, scenarios.size())
                .mapToObj(index -> new ScenarioMessage(
                        UUID.nameUUIDFromBytes((fileId + ":" + index).getBytes(StandardCharsets.UTF_8)),
                        merchantCode, scenarios.get(index)))
                .toList();
    }
}
