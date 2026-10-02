package com.fluts.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reads the scenario topic and stores every scenario in PostgreSQL with input type {@code KAFKA}.
 * A backend service without a web server; the stored scenarios show up in the full app.
 */
@SpringBootApplication
public class FlutConsumerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FlutConsumerApplication.class, args);
    }
}
