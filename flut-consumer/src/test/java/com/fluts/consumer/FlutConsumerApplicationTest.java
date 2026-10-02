package com.fluts.consumer;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Starts the real app through {@code main()}, without connecting to Kafka (see ScenarioConsumerTest for that). */
@Testcontainers
class FlutConsumerApplicationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestConfiguration.IMAGE);

    @Test
    void mainStartsTheApplication() {
        assertThatNoException().isThrownBy(() -> FlutConsumerApplication.main(new String[] {
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--spring.kafka.listener.auto-startup=false",
            "--spring.kafka.admin.auto-create=false",
            "--flut.kafka.pipeline-secret=test-pipeline-secret"}));
    }
}
