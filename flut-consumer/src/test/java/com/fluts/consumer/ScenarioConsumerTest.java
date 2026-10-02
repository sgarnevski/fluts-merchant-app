package com.fluts.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.kafka.ScenarioMessage;
import com.fluts.kafka.ScenarioMessageCodec;
import com.fluts.platform.signing.HmacSigner;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/** The consumer end to end: a real (embedded) Kafka broker, the listener, and PostgreSQL. */
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "flut.consumer.retry-interval=50ms",
    "flut.kafka.pipeline-secret=test-pipeline-secret"})
@EmbeddedKafka
@Import(PostgresTestConfiguration.class)
class ScenarioConsumerTest {

    private static final String TOPIC = "flut.scenarios";
    private static final String DEAD_LETTER_TOPIC = "flut.scenarios.DLT";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Scenario EXAMPLE = new Scenario("From Kafka",
            List.of(new Schuur(List.of(7, 3, 11, 9, 10)), new Schuur(List.of())));

    private final KafkaTemplate<String, String> kafka;
    private final ScenarioMessageCodec codec;
    private final JdbcTemplate jdbc;
    private final EmbeddedKafkaBroker broker;
    private final KafkaAdmin admin;

    ScenarioConsumerTest(@Autowired final KafkaTemplate<String, String> kafka, @Autowired final ScenarioMessageCodec codec,
            @Autowired final JdbcTemplate jdbc, @Autowired final EmbeddedKafkaBroker broker,
            @Autowired final KafkaAdmin admin) {
        this.kafka = kafka;
        this.codec = codec;
        this.jdbc = jdbc;
        this.broker = broker;
        this.admin = admin;
    }

    @Test
    void storesAReceivedScenarioWithInputTypeKafka() {
        final ScenarioMessage message = new ScenarioMessage(UUID.randomUUID(), "dutch", EXAMPLE);

        send(message);

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(stored(message)).containsExactly(
                Map.of("name", "From Kafka", "input_type", "KAFKA", "merchant_code", "dutch")));
        assertThat(jdbc.queryForList("""
                SELECT box.price FROM box JOIN schuur ON schuur.id = box.schuur_id JOIN scenario ON scenario.id = schuur.scenario_id
                WHERE scenario.message_id = ? ORDER BY schuur.position, box.position""", Integer.class, message.messageId()))
                .containsExactly(7, 3, 11, 9, 10);
    }

    @Test
    void storesAMessageDeliveredTwiceOnlyOnce() {
        final ScenarioMessage message = new ScenarioMessage(UUID.randomUUID(), "dutch", EXAMPLE);
        final ScenarioMessage next = new ScenarioMessage(UUID.randomUUID(), "dutch", EXAMPLE);

        // One partition keeps the order: once "next" is stored, both deliveries of "message" were handled.
        kafka.send(TOPIC, 0, codec.key(message), codec.value(message));
        kafka.send(TOPIC, 0, codec.key(message), codec.value(message));
        kafka.send(TOPIC, 0, codec.key(next), codec.value(next));

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(stored(next)).hasSize(1));
        assertThat(stored(message)).hasSize(1);
    }

    @Test
    void sendsMessagesThatAreNotScenariosToTheDeadLetterTopic() {
        final String key = UUID.randomUUID().toString();

        kafka.send(TOPIC, key, "{\"name\": \"Broken\", \"schuurs\": [{\"boxPrices\": [0]}]}");

        assertThat(deadLetter(key).value()).contains("Broken");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM scenario WHERE name = 'Broken'", Integer.class)).isZero();
    }

    @Test
    void sendsMessagesOfUnknownMerchantsToTheDeadLetterTopic() {
        final ScenarioMessage message = new ScenarioMessage(UUID.randomUUID(), "martian", EXAMPLE);

        send(message);

        assertThat(deadLetter(codec.key(message)).value()).isEqualTo(codec.value(message));
        assertThat(stored(message)).isEmpty();
    }

    @Test
    void sendsMessagesNotSignedByThePipelineToTheDeadLetterTopic() {
        final ScenarioMessage message = new ScenarioMessage(UUID.randomUUID(), "dutch", EXAMPLE);
        final ScenarioMessageCodec forger = new ScenarioMessageCodec(new JsonScenarioParser(InputLimits.DEFAULT),
                new HmacSigner("not the pipeline secret"));

        kafka.send(TOPIC, codec.key(message), forger.value(message));

        assertThat(deadLetter(codec.key(message)).value()).contains("\"merchant\":\"dutch\"");
        assertThat(stored(message)).isEmpty();
    }

    @Test
    void sendsMessagesWithoutMessageIdToTheDeadLetterTopic() {
        final ScenarioMessage message = new ScenarioMessage(UUID.randomUUID(), "dutch", new Scenario("No message id", EXAMPLE.schuurs()));

        kafka.send(TOPIC, "not-a-message-id", codec.value(message));

        assertThat(deadLetter("not-a-message-id").value()).isEqualTo(codec.value(message));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM scenario WHERE name = 'No message id'", Integer.class)).isZero();
    }

    @Test
    void retriesWhenTheDatabaseFailsThenGivesUpAndContinues() {
        final ScenarioMessage unlucky = new ScenarioMessage(UUID.randomUUID(), "dutch", EXAMPLE);
        final ScenarioMessage lucky = new ScenarioMessage(UUID.randomUUID(), "dutch", EXAMPLE);
        jdbc.execute("ALTER TABLE scenario RENAME TO scenario_unavailable");
        try {
            send(unlucky);

            assertThat(deadLetter(codec.key(unlucky)).value()).isEqualTo(codec.value(unlucky));
        } finally {
            jdbc.execute("ALTER TABLE scenario_unavailable RENAME TO scenario");
        }

        send(lucky);

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(stored(lucky)).hasSize(1));
        assertThat(stored(unlucky)).isEmpty();
    }

    @Test
    void createsTheTopicsWithBoundedRetention() throws Exception {
        try (AdminClient client = AdminClient.create(admin.getConfigurationProperties())) {
            final ConfigResource topic = new ConfigResource(ConfigResource.Type.TOPIC, TOPIC);
            final Config config = client.describeConfigs(List.of(topic)).all().get().get(topic);

            assertThat(config.get("retention.ms").value()).isEqualTo("21600000");
            assertThat(config.get("retention.bytes").value()).isEqualTo("10485760");
            assertThat(config.get("segment.bytes").value()).isEqualTo("1048576");
            assertThat(client.listTopics().names().get()).contains(TOPIC, DEAD_LETTER_TOPIC);
        }
    }

    private void send(final ScenarioMessage message) {
        kafka.send(TOPIC, codec.key(message), codec.value(message));
    }

    private List<Map<String, Object>> stored(final ScenarioMessage message) {
        return jdbc.queryForList("SELECT name, input_type, merchant_code FROM scenario WHERE message_id = ?", message.messageId());
    }

    /** Waits for the message with this key on the dead-letter topic. */
    private ConsumerRecord<String, String> deadLetter(final String key) {
        final Map<String, Object> properties = KafkaTestUtils.consumerProps(broker, "test-" + UUID.randomUUID(), true);
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(properties,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of(DEAD_LETTER_TOPIC));
            return await().atMost(TIMEOUT).until(
                    () -> StreamSupport.stream(consumer.poll(Duration.ofMillis(200)).spliterator(), false)
                            .filter(record -> key.equals(record.key()))
                            .findFirst()
                            .orElse(null),
                    record -> record != null);
        }
    }
}
