package com.fluts.trader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

/** An SFTP server inside the test JVM (user {@code trader} / {@code trader}) that serves a local directory. */
final class TestSftpServer implements AutoCloseable {

    private final SshServer server = SshServer.setUpDefaultServer();

    TestSftpServer(final Path root) throws IOException {
        server.setHost("localhost");
        server.setPort(0);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.setPasswordAuthenticator((username, password, session) -> "trader".equals(username) && "trader".equals(password));
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(root));
        server.start();
    }

    int port() {
        return server.getPort();
    }

    @Override
    public void close() throws IOException {
        server.stop(true);
    }
}
