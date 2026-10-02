package com.fluts.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Full app: scenarios stored in PostgreSQL, the scenario API and the shared evaluation API
 * (wired in {@link AppConfiguration}).
 */
@SpringBootApplication
public class FlutApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FlutApplication.class, args);
    }
}
