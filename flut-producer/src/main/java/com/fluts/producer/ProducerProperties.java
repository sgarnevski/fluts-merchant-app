package com.fluts.producer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Configurable as {@code flut.producer.*}.
 *
 * @param directory    base directory: {@code <directory>/<merchant>/data/{inbox,processed,failed}}, the
 *                     layout of the SFTP server's home directories
 * @param merchants    the merchants files are accepted from, by code, each with the secret its files are signed with;
 *                     a folder of a merchant that is not configured is ignored
 * @param pollInterval pause between two looks into the inboxes
 * @param settleTime   a file is only taken once it has not changed for this long (it may still be uploading)
 * @param sendTimeout  how long to wait for Kafka to confirm the messages of one file
 * @param maxFileSize  larger files are rejected; keeps every message well below Kafka's 1 MB limit
 */
@ConfigurationProperties("flut.producer")
public record ProducerProperties(
        @DefaultValue("pipeline") Path directory,
        Map<String, Merchant> merchants,
        @DefaultValue("5s") Duration pollInterval,
        @DefaultValue("2s") Duration settleTime,
        @DefaultValue("30s") Duration sendTimeout,
        @DefaultValue("512KB") DataSize maxFileSize) {

    public ProducerProperties {
        merchants = Map.copyOf(merchants);
    }

    /**
     * A merchant that delivers files.
     *
     * @param secret the merchant's signing secret; a file is accepted only with a matching {@code .sig}
     */
    public record Merchant(String secret) {
    }

    /** Where the merchant's files arrive. */
    public Path inbox(final String merchant) {
        return folder(merchant, "inbox");
    }

    /** Where the merchant's files go whose scenarios were all sent. */
    public Path processed(final String merchant) {
        return folder(merchant, "processed");
    }

    /** Where the merchant's invalid files go, each with a {@code .error} file saying what is wrong. */
    public Path failed(final String merchant) {
        return folder(merchant, "failed");
    }

    private Path folder(final String merchant, final String name) {
        return directory.resolve(merchant).resolve("data").resolve(name);
    }
}
