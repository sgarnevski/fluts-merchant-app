package com.fluts.trader;

import com.fluts.platform.signing.HmacSigner;
import java.time.Clock;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on the schedule and wires generator and SFTP upload. Randomness and time are beans, so tests can fix them. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(TraderProperties.class)
public class TraderConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** {@code java.util.Random}: part of java.base, so it exists in every runtime image (the newer algorithms do not). */
    @Bean
    RandomGenerator randomGenerator() {
        return new Random();
    }

    @Bean
    ScenarioFileGenerator scenarioFileGenerator(final RandomGenerator random, final Clock clock,
            final TraderProperties properties) {
        return new ScenarioFileGenerator(random, clock, properties.merchant(), new HmacSigner(properties.secret()),
                properties.invalidFileRatio());
    }

    /** A new connection per delivery. The server's host key is not checked: a demo setup, not for production. */
    @Bean
    SessionFactory<SftpClient.DirEntry> sftpSessionFactory(final TraderProperties properties) {
        final DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(properties.sftp().host());
        factory.setPort(properties.sftp().port());
        factory.setUser(properties.sftp().username());
        factory.setPassword(properties.sftp().password());
        factory.setAllowUnknownKeys(true);
        return factory;
    }

    @Bean
    FileUploader fileUploader(final SessionFactory<SftpClient.DirEntry> sessions, final TraderProperties properties) {
        return new SftpFileUploader(sessions, properties.sftp().directory());
    }
}
