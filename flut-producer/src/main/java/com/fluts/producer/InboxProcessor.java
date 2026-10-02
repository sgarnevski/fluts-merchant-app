package com.fluts.producer;

import com.fluts.domain.Scenario;
import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.io.ScenarioParseException;
import com.fluts.io.ScenarioParser;
import com.fluts.io.TextScenarioParser;
import com.fluts.kafka.ScenarioMessage;
import com.fluts.platform.signing.HmacSigner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Takes the files of every configured merchant's inbox one by one. A file is accepted only with a
 * signature file {@code <name>.sig} that verifies against that merchant's secret — the proof of who
 * created the file. Then:
 * <ul>
 *   <li>valid file: every scenario is published for the merchant, then file and signature move to
 *       {@code processed/};</li>
 *   <li>invalid file (wrong or missing signature, bad content): nothing is published; file and signature
 *       move to {@code failed/} with a {@code <name>.error} file that says what is wrong;</li>
 *   <li>publishing (or moving) fails: the file stays in the inbox and is tried again on the next
 *       poll. Message ids are derived from the file content, so consumers recognise the repeats.</li>
 * </ul>
 * Files still being written are left alone: names ending in {@value #PARTIAL_SUFFIX}, hidden files,
 * and files that changed within the settle time.
 */
@Component
public class InboxProcessor {

    static final String PARTIAL_SUFFIX = ".part";
    static final String SIGNATURE_SUFFIX = ".sig";

    private static final Logger LOG = LoggerFactory.getLogger(InboxProcessor.class);

    private final ProducerProperties properties;
    private final ScenarioPublisher publisher;
    private final Clock clock;

    public InboxProcessor(final ProducerProperties properties, final ScenarioPublisher publisher, final Clock clock) {
        this.properties = properties;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${flut.producer.poll-interval:5s}")
    public void processInboxes() {
        properties.merchants().keySet().stream().sorted().forEach(this::processInbox);
    }

    private void processInbox(final String merchant) {
        for (final Path file : readyFiles(merchant)) {
            try {
                process(merchant, file);
            } catch (final IOException | PublishException e) {
                LOG.warn("Could not process {} of {}; it stays in the inbox and is tried again: {}",
                        file.getFileName(), merchant, e.toString());
            }
        }
    }

    private List<Path> readyFiles(final String merchant) {
        final Instant settledBefore = clock.instant().minus(properties.settleTime());
        try (Stream<Path> files = Files.list(properties.inbox(merchant))) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> !isPartialOrSignature(file))
                    .filter(file -> !Instant.ofEpochMilli(file.toFile().lastModified()).isAfter(settledBefore))
                    .sorted()
                    .toList();
        } catch (final IOException e) {
            LOG.debug("No inbox to read for {} at {}: {}", merchant, properties.inbox(merchant), e.toString());
            return List.of();
        }
    }

    private static boolean isPartialOrSignature(final Path file) {
        final String name = file.getFileName().toString();
        return name.endsWith(PARTIAL_SUFFIX) || name.endsWith(SIGNATURE_SUFFIX) || name.startsWith(".");
    }

    private void process(final String merchant, final Path file) throws IOException {
        final String name = file.getFileName().toString();
        try {
            final byte[] content = readSigned(merchant, file);
            final List<Scenario> scenarios = parserFor(name).parse(new ByteArrayInputStream(content));
            final List<ScenarioMessage> messages = ScenarioMessage.forFile(merchant, content, scenarios);
            publisher.publish(messages);
            move(file, properties.processed(merchant));
            LOG.info("Sent {} scenario(s) of {} for {}", messages.size(), name, merchant);
        } catch (final ScenarioParseException e) {
            final Path target = move(file, properties.failed(merchant));
            Files.writeString(target.resolveSibling(name + ".error"), e.getMessage() + "\n");
            LOG.warn("Rejected {} of {}: {}", name, merchant, e.getMessage());
        }
    }

    /** The file's bytes, once its signature file proves the merchant created it. */
    private byte[] readSigned(final String merchant, final Path file) throws IOException {
        if (Files.size(file) > properties.maxFileSize().toBytes()) {
            throw ScenarioParseException.of("File is larger than " + properties.maxFileSize());
        }
        final byte[] content = Files.readAllBytes(file);
        final String signature;
        try {
            signature = Files.readString(signatureOf(file));
        } catch (final NoSuchFileException e) {
            throw ScenarioParseException.of("No signature file " + signatureOf(file).getFileName()
                    + ": every file must come with the merchant's HMAC-SHA256 signature");
        }
        if (!new HmacSigner(properties.merchants().get(merchant).secret()).verify(content, signature)) {
            throw ScenarioParseException.of("Signature does not match the " + merchant + " merchant's secret");
        }
        return content;
    }

    private static Path signatureOf(final Path file) {
        return file.resolveSibling(file.getFileName() + SIGNATURE_SUFFIX);
    }

    /** Scenarios without a name of their own are named after the file: {@code "<file name> #<position>"}. */
    private static ScenarioParser parserFor(final String name) {
        final IntFunction<String> defaultName = position -> name + " #" + position;
        final String lowerCaseName = name.toLowerCase(Locale.ROOT);
        if (lowerCaseName.endsWith(".json")) {
            return new JsonScenarioParser(InputLimits.DEFAULT, defaultName);
        }
        if (lowerCaseName.endsWith(".txt")) {
            return new TextScenarioParser(InputLimits.DEFAULT, defaultName);
        }
        throw ScenarioParseException.of("Unsupported file '" + name + "': deliver .txt or .json files");
    }

    /** Moves the file and, if there is one, its signature file. */
    private static Path move(final Path file, final Path directory) throws IOException {
        Files.createDirectories(directory);
        final Path signature = signatureOf(file);
        if (Files.exists(signature)) {
            Files.move(signature, directory.resolve(signature.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.move(file, directory.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
}
