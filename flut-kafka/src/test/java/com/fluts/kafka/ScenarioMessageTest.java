package com.fluts.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.io.ScenarioParseException;
import com.fluts.platform.signing.HmacSigner;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScenarioMessageTest {

    private static final Scenario FIRST = new Scenario("First", List.of(new Schuur(List.of(7, 3)), new Schuur(List.of())));
    private static final Scenario SECOND = new Scenario("Second", List.of(new Schuur(List.of(12))));

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    private final ScenarioMessageCodec codec = new ScenarioMessageCodec(new JsonScenarioParser(InputLimits.DEFAULT),
            new HmacSigner("pipeline-secret"));
    private final ScenarioMessageCodec forger = new ScenarioMessageCodec(new JsonScenarioParser(InputLimits.DEFAULT),
            new HmacSigner("guessed-secret"));

    @Test
    void theSameFileAlwaysGivesTheSameMessageIds() {
        final List<ScenarioMessage> messages = ScenarioMessage.forFile("dutch", bytes("1\n2 7 3\n"), List.of(FIRST, SECOND));
        final List<ScenarioMessage> again = ScenarioMessage.forFile("dutch", bytes("1\n2 7 3\n"), List.of(FIRST, SECOND));

        assertThat(messages).extracting(ScenarioMessage::scenario).containsExactly(FIRST, SECOND);
        assertThat(messages).extracting(ScenarioMessage::merchantCode).containsOnly("dutch");
        assertThat(messages).isEqualTo(again);
        assertThat(messages.get(0).messageId()).isNotEqualTo(messages.get(1).messageId());
    }

    @Test
    void differentFilesGiveDifferentMessageIds() {
        assertThat(ScenarioMessage.forFile("dutch", bytes("a"), List.of(FIRST)).getFirst().messageId())
                .isNotEqualTo(ScenarioMessage.forFile("dutch", bytes("b"), List.of(FIRST)).getFirst().messageId());
    }

    @Test
    void encodesTheIdAsKeyAndASignedEnvelopeAsValue() {
        final ScenarioMessage message = new ScenarioMessage(ID, "dutch", FIRST);

        assertThat(codec.key(message)).isEqualTo("00000000-0000-0000-0000-000000000042");
        assertThat(codec.value(message))
                .startsWith("{\"merchant\":\"dutch\",\"scenario\":{\"name\":\"First\",\"schuurs\":[{\"boxPrices\":[7,3]},{\"boxPrices\":[]}]},\"signature\":\"")
                .matches(".*\"signature\":\"[0-9a-f]{64}\"}$");
        assertThat(codec.decode(codec.key(message), codec.value(message))).isEqualTo(message);
    }

    @Test
    void rejectsMessagesNotSignedByThePipeline() {
        final ScenarioMessage message = new ScenarioMessage(ID, "dutch", FIRST);
        final String key = codec.key(message);

        assertThatThrownBy(() -> codec.decode(key, forger.value(message)))
                .isInstanceOf(ScenarioParseException.class)
                .hasMessage("Message signature does not verify: not sent by the pipeline");
        assertThatThrownBy(() -> codec.decode(key, codec.value(message).replace("\"dutch\"", "\"french\"")))
                .as("the merchant is covered by the signature")
                .hasMessage("Message signature does not verify: not sent by the pipeline");
        assertThatThrownBy(() -> codec.decode(key, codec.value(message).replace("[7,3]", "[7,2]")))
                .as("the scenario is covered by the signature")
                .hasMessage("Message signature does not verify: not sent by the pipeline");
        assertThatThrownBy(() -> codec.decode(UUID.randomUUID().toString(), codec.value(message)))
                .as("the message id is covered by the signature")
                .hasMessage("Message signature does not verify: not sent by the pipeline");
        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \"dutch\", \"scenario\": {\"name\": \"First\", \"schuurs\": [{\"boxPrices\": [7, 3]}, {\"boxPrices\": []}]}}"))
                .as("no signature").hasMessage("Message signature does not verify: not sent by the pipeline");
    }

    @Test
    void rejectsMessagesThatAreNotScenarioEnvelopes() {
        final String key = ID.toString();

        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \"dutch\", \"scenario\": {\"name\": \"x\", \"schuurs\": [{\"boxPrices\": [0]}]}}"))
                .isInstanceOf(ScenarioParseException.class)
                .hasMessage("Scenario 1, schuur 1: box price must be positive, got 0");
        assertThatThrownBy(() -> codec.decode(key, "not json")).isInstanceOf(ScenarioParseException.class)
                .hasMessageStartingWith("Invalid message: ");
        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \"dutch\", \"unexpected\": 1}"))
                .hasMessageStartingWith("Invalid message: ");
        assertThatThrownBy(() -> codec.decode(key, "{\"scenario\": {\"schuurs\": []}}"))
                .hasMessage("Message names no merchant");
        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \" \", \"scenario\": {\"schuurs\": []}}"))
                .hasMessage("Message names no merchant");
        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \"dutch\"}"))
                .hasMessage("Scenario 1 is empty");
        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \"dutch\", \"scenario\": {\"schuurs\": [], \"unexpected\": 1}}"))
                .hasMessageStartingWith("Invalid scenario: ");
        assertThatThrownBy(() -> codec.decode(key, "{\"merchant\": \"dutch\", \"scenario\": {\"schuurs\": [{\"boxPrices\": [\"5\"]}]}}"))
                .as("numbers written as strings are refused, as by the core's parser")
                .hasMessageStartingWith("Invalid scenario: ");
        assertThatThrownBy(() -> codec.decode(key, null))
                .isInstanceOf(ScenarioParseException.class).hasMessage("Message has no value");
    }

    @Test
    void rejectsKeysThatAreNotMessageIds() {
        final String value = codec.value(new ScenarioMessage(ID, "dutch", FIRST));

        assertThatThrownBy(() -> codec.decode("42", value))
                .isInstanceOf(ScenarioParseException.class)
                .hasMessage("Message key '42' is not a message id (UUID)");
        assertThatThrownBy(() -> codec.decode(null, "{}"))
                .isInstanceOf(ScenarioParseException.class)
                .hasMessage("Message key 'null' is not a message id (UUID)");
    }

    private static byte[] bytes(final String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
