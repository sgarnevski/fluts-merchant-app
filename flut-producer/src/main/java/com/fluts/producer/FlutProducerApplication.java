package com.fluts.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Watches the inbox directory for scenario files ({@code .txt} in the original format, {@code .json}
 * normalized) and sends every scenario as one message to Kafka. The directory is a plain directory:
 * how files get there (e.g. an SFTP server writing to the same volume) is not this app's concern.
 */
@SpringBootApplication
public class FlutProducerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FlutProducerApplication.class, args);
    }
}
