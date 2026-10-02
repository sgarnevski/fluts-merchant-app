package com.fluts.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.kafka.ScenarioMessage;
import com.fluts.platform.signing.HmacSigner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

class InboxProcessorTest {

    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");
    private static final Instant LONG_AGO = NOW.minusSeconds(60);
    private static final HmacSigner DUTCH = new HmacSigner("dutch-secret");
    private static final HmacSigner FRENCH = new HmacSigner("french-secret");

    private final Path directory;
    private final ProducerProperties properties;
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final InboxProcessor processor;

    InboxProcessorTest(@TempDir final Path directory) {
        this.directory = directory;
        this.properties = new ProducerProperties(directory,
                Map.of("dutch", new ProducerProperties.Merchant("dutch-secret"),
                        "french", new ProducerProperties.Merchant("french-secret")),
                Duration.ofSeconds(5), Duration.ofSeconds(2), Duration.ofSeconds(30), DataSize.ofBytes(200));
        this.processor = new InboxProcessor(properties, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @BeforeEach
    void createInboxes() throws IOException {
        Files.createDirectories(properties.inbox("dutch"));
        Files.createDirectories(properties.inbox("french"));
    }

    @Test
    void sendsEveryScenarioOfASignedTextFileForItsMerchantNamedAfterTheFile() throws IOException {
        deliver("dutch", DUTCH, "batch.txt", "1\n2 7 3\n2\n1 5\n0\n0\n");

        processor.processInboxes();

        assertThat(publisher.published).extracting(ScenarioMessage::merchantCode).containsOnly("dutch");
        assertThat(publisher.published).extracting(ScenarioMessage::scenario).containsExactly(
                new Scenario("batch.txt #1", List.of(new Schuur(List.of(7, 3)))),
                new Scenario("batch.txt #2", List.of(new Schuur(List.of(5)), new Schuur(List.of()))));
        assertThat(properties.inbox("dutch")).isEmptyDirectory();
        assertThat(properties.processed("dutch").resolve("batch.txt")).hasContent("1\n2 7 3\n2\n1 5\n0\n0\n");
        assertThat(properties.processed("dutch").resolve("batch.txt.sig")).exists();
    }

    @Test
    void handlesEveryMerchantsInboxWithItsOwnSecret() throws IOException {
        deliver("french", FRENCH, "Batch.JSON", "{\"scenarios\":[{\"name\":\"Own\",\"schuurs\":[{\"boxPrices\":[1]}]},{\"schuurs\":[{\"boxPrices\":[]}]}]}");
        deliver("dutch", DUTCH, "a.txt", "1\n1 4\n");

        processor.processInboxes();

        assertThat(publisher.published).extracting(ScenarioMessage::merchantCode, message -> message.scenario().name())
                .containsExactly(
                        tuple("dutch", "a.txt #1"),
                        tuple("french", "Own"),
                        tuple("french", "Batch.JSON #2"));
        assertThat(properties.processed("french").resolve("Batch.JSON")).exists();
    }

    @Test
    void rejectsAFileSignedWithAnotherMerchantsSecret() throws IOException {
        deliver("dutch", FRENCH, "forged.txt", "1\n1 5\n");

        processor.processInboxes();

        assertThat(publisher.published).isEmpty();
        assertThat(properties.inbox("dutch")).isEmptyDirectory();
        assertThat(properties.failed("dutch").resolve("forged.txt")).exists();
        assertThat(properties.failed("dutch").resolve("forged.txt.sig")).exists();
        assertThat(properties.failed("dutch").resolve("forged.txt.error"))
                .hasContent("Signature does not match the dutch merchant's secret\n");
    }

    @Test
    void rejectsAFileWithoutSignature() throws IOException {
        final Path file = Files.writeString(properties.inbox("dutch").resolve("unsigned.txt"), "1\n1 5\n");
        Files.setLastModifiedTime(file, FileTime.from(LONG_AGO));

        processor.processInboxes();

        assertThat(publisher.published).isEmpty();
        assertThat(properties.failed("dutch").resolve("unsigned.txt.error"))
                .hasContent("No signature file unsigned.txt.sig: every file must come with the merchant's HMAC-SHA256 signature\n");
    }

    @Test
    void movesAnInvalidFileToFailedAndSaysWhatIsWrong() throws IOException {
        deliver("dutch", DUTCH, "bad.txt", "1\n3 1 2\n");

        processor.processInboxes();

        assertThat(publisher.published).isEmpty();
        assertThat(properties.inbox("dutch")).isEmptyDirectory();
        assertThat(properties.failed("dutch").resolve("bad.txt")).hasContent("1\n3 1 2\n");
        assertThat(properties.failed("dutch").resolve("bad.txt.error"))
                .hasContent("Line 2: Schuur declares 3 boxes but lists 2 prices\n");
    }

    @Test
    void rejectsOtherFileTypes() throws IOException {
        deliver("dutch", DUTCH, "notes.csv", "1;2;3");

        processor.processInboxes();

        assertThat(properties.failed("dutch").resolve("notes.csv.error"))
                .hasContent("Unsupported file 'notes.csv': deliver .txt or .json files\n");
    }

    @Test
    void rejectsFilesThatAreTooLarge() throws IOException {
        deliver("dutch", DUTCH, "huge.txt", "1\n100 " + "5 ".repeat(100) + "\n");

        processor.processInboxes();

        assertThat(publisher.published).isEmpty();
        assertThat(properties.failed("dutch").resolve("huge.txt.error")).hasContent("File is larger than 200B\n");
    }

    @Test
    void leavesFilesAloneThatMayStillBeWritten() throws IOException {
        deliver("dutch", DUTCH, "upload.txt.part", "1\n1 5\n");
        deliver("dutch", DUTCH, ".hidden.txt", "1\n1 5\n");
        Files.createDirectory(properties.inbox("dutch").resolve("folder.txt"));
        final Path fresh = deliver("dutch", DUTCH, "fresh.txt", "1\n1 5\n");
        Files.setLastModifiedTime(fresh, FileTime.from(NOW.minusSeconds(1)));

        processor.processInboxes();

        assertThat(publisher.published).isEmpty();
        assertThat(properties.inbox("dutch").resolve("fresh.txt")).exists();

        new InboxProcessor(properties, publisher, Clock.fixed(NOW.plusSeconds(5), ZoneOffset.UTC)).processInboxes();

        assertThat(publisher.published).hasSize(1);
        assertThat(properties.inbox("dutch").resolve("fresh.txt")).doesNotExist();
        assertThat(properties.inbox("dutch").resolve("upload.txt.part")).exists();
        assertThat(properties.inbox("dutch").resolve(".hidden.txt")).exists();
    }

    @Test
    void keepsAFileInTheInboxWhenSendingFailsAndSendsItOnTheNextPoll() throws IOException {
        deliver("dutch", DUTCH, "a-first.txt", "1\n1 5\n");
        deliver("dutch", DUTCH, "b-second.txt", "1\n1 6\n");
        publisher.failures.add("a-first.txt #1");

        processor.processInboxes();

        assertThat(properties.inbox("dutch").resolve("a-first.txt")).exists();
        assertThat(properties.inbox("dutch").resolve("a-first.txt.sig")).exists();
        assertThat(properties.processed("dutch").resolve("b-second.txt")).exists();

        publisher.failures.clear();
        processor.processInboxes();

        assertThat(properties.processed("dutch").resolve("a-first.txt")).exists();
        assertThat(publisher.published).extracting(message -> message.scenario().name())
                .containsExactly("b-second.txt #1", "a-first.txt #1");
    }

    @Test
    void keepsAFileInTheInboxWhenItCannotBeMoved() throws IOException {
        deliver("dutch", DUTCH, "stuck.txt", "1\n1 5\n");
        Files.writeString(properties.processed("dutch"), "a file where the processed folder should be");

        processor.processInboxes();

        assertThat(properties.inbox("dutch").resolve("stuck.txt")).exists();
    }

    @Test
    void ignoresFoldersOfMerchantsThatAreNotConfiguredAndMissingInboxes() throws IOException {
        Files.delete(properties.inbox("french"));
        final Path stranger = Files.createDirectories(directory.resolve("martian/data/inbox"));
        deliver("dutch", DUTCH, "ok.txt", "1\n1 5\n");
        Files.writeString(stranger.resolve("mars.txt"), "1\n1 5\n");
        Files.writeString(stranger.resolve("mars.txt.sig"), DUTCH.sign("1\n1 5\n"));

        processor.processInboxes();

        assertThat(publisher.published).extracting(ScenarioMessage::merchantCode).containsExactly("dutch");
        assertThat(stranger.resolve("mars.txt")).exists();
    }

    /** Writes a signed file into the merchant's inbox, finished long ago. */
    private Path deliver(final String merchant, final HmacSigner signer, final String name, final String content) throws IOException {
        final Path inbox = properties.inbox(merchant);
        Files.writeString(inbox.resolve(name + ".sig"), signer.sign(content.getBytes(StandardCharsets.UTF_8)));
        final Path file = Files.writeString(inbox.resolve(name), content);
        Files.setLastModifiedTime(file, FileTime.from(LONG_AGO));
        return file;
    }

    /** Remembers what was published; fails for batches that contain a scenario named in {@code failures}. */
    private static final class RecordingPublisher implements ScenarioPublisher {

        private final List<ScenarioMessage> published = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();

        @Override
        public void publish(final List<ScenarioMessage> messages) {
            if (messages.stream().anyMatch(message -> failures.contains(message.scenario().name()))) {
                throw new PublishException("Kafka is down", new IllegalStateException("no broker"));
            }
            published.addAll(messages);
        }
    }
}
