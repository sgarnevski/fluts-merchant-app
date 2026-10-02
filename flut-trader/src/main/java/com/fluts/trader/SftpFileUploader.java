package com.fluts.trader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

/**
 * Uploads over SFTP, one connection per delivery. For each file the signature {@code <name>.sig}
 * goes first; the file itself is written as {@code <name>.part} and renamed when complete, so a
 * reader of the directory never sees half a file, and never a file without its signature.
 */
public final class SftpFileUploader implements FileUploader {

    static final String PARTIAL_SUFFIX = ".part";

    private final SessionFactory<SftpClient.DirEntry> sessions;
    private final String directory;

    public SftpFileUploader(final SessionFactory<SftpClient.DirEntry> sessions, final String directory) {
        this.sessions = sessions;
        this.directory = directory;
    }

    private static ByteArrayInputStream bytes(final String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void upload(final List<GeneratedFile> files) {
        try (Session<SftpClient.DirEntry> session = sessions.getSession()) {
            for (final GeneratedFile file : files) {
                final String target = directory + "/" + file.name();
                session.write(bytes(file.signature()), directory + "/" + file.signatureName());
                session.write(bytes(file.content()), target + PARTIAL_SUFFIX);
                session.rename(target + PARTIAL_SUFFIX, target);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Upload to " + directory + " failed", e);
        }
    }
}
