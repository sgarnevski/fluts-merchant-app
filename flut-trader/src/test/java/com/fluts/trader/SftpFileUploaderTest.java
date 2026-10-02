package com.fluts.trader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/** Uploads to a real SFTP server running inside the test. */
class SftpFileUploaderTest {

    private static final List<GeneratedFile> FILES = List.of(
            new GeneratedFile("first.txt", "1\n1 5\n0\n", "0123abcd"),
            new GeneratedFile("second.json", "{\"scenarios\": []}\n", "4567ef01"));

    private final Path root;

    SftpFileUploaderTest(@TempDir final Path root) {
        this.root = root;
    }

    @Test
    void uploadsCompleteFilesUnderTheirFinalNames() throws IOException {
        final Path inbox = Files.createDirectories(root.resolve("data/inbox"));
        try (TestSftpServer server = new TestSftpServer(root)) {
            uploader(server.port(), "trader").upload(FILES);
        }

        assertThat(inbox).isDirectoryContaining(file -> file.getFileName().toString().equals("first.txt"));
        assertThat(inbox.resolve("first.txt")).hasContent("1\n1 5\n0\n");
        assertThat(inbox.resolve("second.json")).hasContent("{\"scenarios\": []}\n");
        assertThat(inbox.resolve("first.txt.sig")).hasContent("0123abcd");
        assertThat(inbox.resolve("second.json.sig")).hasContent("4567ef01");
        assertThat(inbox).isDirectoryNotContaining(file -> file.getFileName().toString().endsWith(".part"));
    }

    @Test
    void failsWhenTheRemoteDirectoryIsMissing() throws IOException {
        try (TestSftpServer server = new TestSftpServer(root)) {
            assertThatThrownBy(() -> uploader(server.port(), "trader").upload(FILES))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessage("Upload to data/inbox failed");
        }
    }

    @Test
    void failsWhenTheLoginIsRejected() throws IOException {
        try (TestSftpServer server = new TestSftpServer(root)) {
            assertThatThrownBy(() -> uploader(server.port(), "wrong password").upload(FILES))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private static SftpFileUploader uploader(final int port, final String password) {
        final DefaultSftpSessionFactory sessions = new DefaultSftpSessionFactory();
        sessions.setHost("localhost");
        sessions.setPort(port);
        sessions.setUser("trader");
        sessions.setPassword(password);
        sessions.setAllowUnknownKeys(true);
        return new SftpFileUploader(sessions, "data/inbox");
    }
}
