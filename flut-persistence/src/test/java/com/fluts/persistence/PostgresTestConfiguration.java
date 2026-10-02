package com.fluts.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL 16 in Docker for tests; Spring Boot connects to it via {@link ServiceConnection}. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    public static final String IMAGE = "postgres:16-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(IMAGE);
    }
}
