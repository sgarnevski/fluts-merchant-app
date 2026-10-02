package com.fluts.trader;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configurable as {@code flut.trader.*}.
 *
 * @param merchant         the merchant this trader delivers for; part of file and scenario names
 * @param secret           the merchant's signing secret: every file is delivered with its HMAC-SHA256 signature
 * @param interval         time between two deliveries
 * @param initialDelay     time before the first delivery (lets the SFTP server start)
 * @param minFiles         fewest files per delivery
 * @param maxFiles         most files per delivery
 * @param invalidFileRatio share of deliberately broken files, 0 to 1; shows the pipeline's {@code failed/} folder
 * @param sftp             where to deliver
 */
@ConfigurationProperties("flut.trader")
public record TraderProperties(
        String merchant,
        String secret,
        @DefaultValue("2m") Duration interval,
        @DefaultValue("5s") Duration initialDelay,
        @DefaultValue("1") int minFiles,
        @DefaultValue("5") int maxFiles,
        @DefaultValue("0") double invalidFileRatio,
        @DefaultValue Sftp sftp) {

    public TraderProperties {
        if (minFiles < 1 || maxFiles < minFiles) {
            throw new IllegalArgumentException(
                    "Need 1 <= min-files <= max-files, got min-files=" + minFiles + ", max-files=" + maxFiles);
        }
    }

    /**
     * @param host      SFTP server
     * @param port      its port
     * @param username  login
     * @param password  password
     * @param directory remote directory the files are delivered to; must exist
     */
    public record Sftp(
            @DefaultValue("localhost") String host,
            @DefaultValue("2222") int port,
            @DefaultValue("trader") String username,
            @DefaultValue("trader") String password,
            @DefaultValue("data/inbox") String directory) {
    }
}
