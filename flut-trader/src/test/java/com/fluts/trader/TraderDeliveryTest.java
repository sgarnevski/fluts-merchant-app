package com.fluts.trader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The trader end to end: the running app keeps delivering files to an SFTP server. */
@SpringBootTest(properties = {"flut.trader.interval=300ms", "flut.trader.initial-delay=0s",
    "flut.trader.merchant=french", "flut.trader.secret=french-secret", "flut.trader.sftp.username=trader",
    "flut.trader.sftp.password=trader"})
class TraderDeliveryTest {

    private static final Path ROOT = createInbox();
    private static final TestSftpServer SERVER = startServer();

    @DynamicPropertySource
    static void sftpServer(final DynamicPropertyRegistry registry) {
        registry.add("flut.trader.sftp.port", SERVER::port);
    }

    @AfterAll
    static void stopServer() throws IOException {
        SERVER.close();
    }

    @Test
    void deliversFilesAgainAndAgain() throws IOException {
        // Every delivery has at most five files, so eleven files mean at least three deliveries.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(deliveredFiles()).hasSizeGreaterThan(10));

        assertThat(deliveredFiles()).allMatch(name -> name.matches("french-\\d{8}-\\d{9}-[1-5]\\.(txt|json)"));
        // The trader keeps delivering, and signatures go up first: check each file has its own, not the counts.
        final List<String> files = deliveredFiles();
        final List<String> signatures = signatureFiles();
        assertThat(files).allSatisfy(file -> assertThat(signatures).contains(file + ".sig"));
    }

    private static List<String> deliveredFiles() throws IOException {
        return inboxFiles().stream().filter(name -> !name.endsWith(".part") && !name.endsWith(".sig")).toList();
    }

    private static List<String> signatureFiles() throws IOException {
        return inboxFiles().stream().filter(name -> name.endsWith(".sig")).toList();
    }

    private static List<String> inboxFiles() throws IOException {
        try (Stream<Path> files = Files.list(ROOT.resolve("data/inbox"))) {
            return files.map(file -> file.getFileName().toString()).toList();
        }
    }

    private static Path createInbox() {
        try {
            final Path root = Files.createTempDirectory("flut-trader");
            Files.createDirectories(root.resolve("data/inbox"));
            return root;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static TestSftpServer startServer() {
        try {
            return new TestSftpServer(ROOT);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
