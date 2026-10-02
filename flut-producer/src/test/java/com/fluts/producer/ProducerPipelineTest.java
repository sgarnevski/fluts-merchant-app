package com.fluts.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.fluts.kafka.ScenarioMessage;
import com.fluts.kafka.ScenarioMessageCodec;
import com.fluts.platform.signing.HmacSigner;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The producer end to end: a file dropped into the inbox arrives as messages on a real (embedded) broker. */
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "flut.producer.poll-interval=200ms",
    "flut.producer.settle-time=0s",
    "flut.producer.merchants.dutch.secret=dutch-secret",
    "flut.kafka.pipeline-secret=test-pipeline-secret"})
@EmbeddedKafka
class ProducerPipelineTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Path DIRECTORY = createTempDirectory();

    private final ProducerProperties properties;
    private final ScenarioMessageCodec codec;
    private final EmbeddedKafkaBroker broker;

    ProducerPipelineTest(@Autowired final ProducerProperties properties, @Autowired final ScenarioMessageCodec codec,
            @Autowired final EmbeddedKafkaBroker broker) {
        this.properties = properties;
        this.codec = codec;
        this.broker = broker;
    }

    @DynamicPropertySource
    static void directory(final DynamicPropertyRegistry registry) {
        registry.add("flut.producer.directory", DIRECTORY::toString);
    }

    @Test
    void sendsTheScenariosOfADeliveredFileAndArchivesIt() throws IOException {
        final Path inbox = Files.createDirectories(properties.inbox("dutch"));
        final byte[] content = Files.readAllBytes(Path.of("../demo/input.txt"));
        // Delivered like the trader does: the signature first, the file under a temporary name, then renamed.
        Files.writeString(inbox.resolve("input.txt.sig"), new HmacSigner("dutch-secret").sign(content));
        Files.move(Files.write(inbox.resolve("input.txt.part"), content), inbox.resolve("input.txt"));

        final List<ConsumerRecord<String, String>> records = receive(2);

        assertThat(records).extracting(record -> codec.decode(record.key(), record.value()))
                .extracting(ScenarioMessage::merchantCode, message -> message.scenario().name())
                .containsExactlyInAnyOrder(tuple("dutch", "input.txt #1"), tuple("dutch", "input.txt #2"));
        assertThat(records).extracting(record -> UUID.fromString(record.key())).doesNotHaveDuplicates();
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(properties.processed("dutch").resolve("input.txt")).exists());
        assertThat(inbox).isEmptyDirectory();
    }

    /** Reads the scenario topic from the beginning until it has {@code count} records. */
    private List<ConsumerRecord<String, String>> receive(final int count) {
        final Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(broker, "test-" + UUID.randomUUID(), true);
        final List<ConsumerRecord<String, String>> records = new ArrayList<>();
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of("flut.scenarios"));
            await().atMost(TIMEOUT).until(() -> {
                consumer.poll(Duration.ofMillis(200)).forEach(records::add);
                return records.size() >= count;
            });
        }
        return records;
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("flut-producer");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
