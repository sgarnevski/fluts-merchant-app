package com.fluts.producer;

import static org.assertj.core.api.Assertions.assertThatNoException;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Starts the real app through {@code main()}, without connecting to Kafka (see ProducerPipelineTest for that). */
class FlutProducerApplicationTest {

    @Test
    void mainStartsTheApplication(@TempDir final Path directory) {
        assertThatNoException().isThrownBy(() -> FlutProducerApplication.main(new String[] {
            "--flut.producer.directory=" + directory,
            "--spring.kafka.admin.auto-create=false",
            "--flut.kafka.pipeline-secret=test-pipeline-secret"}));
    }
}
