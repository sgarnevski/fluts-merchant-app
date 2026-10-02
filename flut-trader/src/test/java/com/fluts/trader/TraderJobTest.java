package com.fluts.trader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fluts.platform.signing.HmacSigner;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TraderJobTest {

    private static final TraderProperties.Sftp SFTP = new TraderProperties.Sftp("localhost", 2222, "trader", "trader", "data/inbox");
    private static final TraderProperties PROPERTIES =
            new TraderProperties("dutch", "dutch-secret", Duration.ofMinutes(2), Duration.ofSeconds(5), 1, 5, 0, SFTP);

    private final List<List<GeneratedFile>> deliveries = new ArrayList<>();
    private final Random random = new Random(3);
    private final ScenarioFileGenerator generator =
            new ScenarioFileGenerator(random, Clock.systemUTC(), "dutch", new HmacSigner("dutch-secret"), 0);

    @Test
    void deliversBetweenOneAndFiveFilesEachTime() {
        final TraderJob job = new TraderJob(PROPERTIES, generator, deliveries::add, random);

        IntStream.range(0, 100).forEach(run -> job.deliver());

        assertThat(deliveries).hasSize(100).allSatisfy(files -> assertThat(files).hasSizeBetween(1, 5));
        assertThat(deliveries).extracting(List::size).contains(1, 2, 3, 4, 5);
    }

    @Test
    void aFailedDeliveryDoesNotStopTheSchedule() {
        final TraderJob job = new TraderJob(PROPERTIES, generator, files -> {
            throw new IllegalStateException("SFTP server is down");
        }, random);

        assertThatNoException().isThrownBy(job::deliver);
    }

    @Test
    void rejectsImpossibleFileCounts() {
        assertThatThrownBy(() -> new TraderProperties("dutch", "s", Duration.ofMinutes(2), Duration.ZERO, 0, 5, 0, SFTP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraderProperties("dutch", "s", Duration.ofMinutes(2), Duration.ZERO, 3, 2, 0, SFTP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Need 1 <= min-files <= max-files, got min-files=3, max-files=2");
    }
}
