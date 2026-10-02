package com.fluts.kafka;

import com.fluts.domain.Scenario;
import com.fluts.io.JsonScenarioParser;
import com.fluts.io.ScenarioInput;
import com.fluts.io.ScenarioParseException;
import com.fluts.io.ScenariosInput;
import com.fluts.platform.io.JsonScenarioWriter;
import com.fluts.platform.signing.HmacSigner;
import java.util.Collections;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The wire format of a {@link ScenarioMessage}: the record key is the message id, the record value
 * an envelope {@code {"merchant": "dutch", "scenario": {...}, "signature": "<hex>"}}.
 *
 * <p>The signature is the pipeline's HMAC over message id, merchant and the scenario in its
 * canonical JSON form. Only holders of the pipeline secret can produce a message the consumer
 * accepts, so the merchant of a message cannot be forged by anyone else who can write to the topic.
 */
public final class ScenarioMessageCodec {

    /** As strict as the core's JSON parser: unknown properties, numbers as strings and fractions are errors. */
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    private final JsonScenarioWriter writer = new JsonScenarioWriter();
    private final JsonScenarioParser parser;
    private final HmacSigner pipelineSigner;

    /**
     * @param parser         validates received scenarios (same rules and limits as every other input path)
     * @param pipelineSigner signs with / verifies against the pipeline secret
     */
    public ScenarioMessageCodec(final JsonScenarioParser parser, final HmacSigner pipelineSigner) {
        this.parser = parser;
        this.pipelineSigner = pipelineSigner;
    }

    public String key(final ScenarioMessage message) {
        return message.messageId().toString();
    }

    public String value(final ScenarioMessage message) {
        final String scenario = writer.writeScenario(message.scenario());
        return MAPPER.writeValueAsString(new Envelope(message.merchantCode(), MAPPER.readTree(scenario),
                pipelineSigner.sign(signedContent(message.messageId(), message.merchantCode(), scenario))));
    }

    /**
     * @throws ScenarioParseException if the key is not a message id, the value is not a valid envelope
     *                                or the signature does not verify
     */
    public ScenarioMessage decode(final @Nullable String key, final @Nullable String value) {
        if (value == null) {
            throw ScenarioParseException.of("Message has no value");
        }
        final UUID messageId = messageId(key);
        final Envelope envelope = readEnvelope(value);
        if (envelope.merchant() == null || envelope.merchant().isBlank()) {
            throw ScenarioParseException.of("Message names no merchant");
        }
        final Scenario scenario = toScenario(envelope.scenario());
        final String signed = signedContent(messageId, envelope.merchant(), writer.writeScenario(scenario));
        if (envelope.signature() == null || !pipelineSigner.verify(signed, envelope.signature())) {
            throw ScenarioParseException.of("Message signature does not verify: not sent by the pipeline");
        }
        return new ScenarioMessage(messageId, envelope.merchant(), scenario);
    }

    /** One scenario, validated by the core's JSON parser (same rules and limits as every other input path). */
    private Scenario toScenario(final @Nullable JsonNode node) {
        final ScenarioInput input = readScenario(node);
        return parser.toScenarios(new ScenariosInput(Collections.singletonList(input))).getFirst();
    }

    private static @Nullable ScenarioInput readScenario(final @Nullable JsonNode node) {
        try {
            return node == null ? null : MAPPER.treeToValue(node, ScenarioInput.class);
        } catch (final JacksonException e) {
            throw ScenarioParseException.of("Invalid scenario: " + e.getOriginalMessage());
        }
    }

    private static String signedContent(final UUID messageId, final String merchant, final String scenarioJson) {
        return messageId + "\n" + merchant + "\n" + scenarioJson;
    }

    private static Envelope readEnvelope(final String value) {
        try {
            return MAPPER.readValue(value, Envelope.class);
        } catch (final JacksonException e) {
            throw ScenarioParseException.of("Invalid message: " + e.getOriginalMessage());
        }
    }

    private static UUID messageId(final @Nullable String key) {
        try {
            return UUID.fromString(String.valueOf(key));
        } catch (final IllegalArgumentException e) {
            throw ScenarioParseException.of("Message key '" + key + "' is not a message id (UUID)");
        }
    }

    /** The JSON envelope; fields may be missing in a received message. */
    private record Envelope(@Nullable String merchant, @Nullable JsonNode scenario, @Nullable String signature) {
    }
}
