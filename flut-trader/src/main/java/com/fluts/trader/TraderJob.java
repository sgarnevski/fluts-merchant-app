package com.fluts.trader;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Delivers a batch of files at a fixed rate. A failed delivery is logged; the next one is tried as scheduled. */
@Component
public class TraderJob {

    private static final Logger LOG = LoggerFactory.getLogger(TraderJob.class);

    private final TraderProperties properties;
    private final ScenarioFileGenerator generator;
    private final FileUploader uploader;
    private final RandomGenerator random;

    public TraderJob(final TraderProperties properties, final ScenarioFileGenerator generator,
            final FileUploader uploader, final RandomGenerator random) {
        this.properties = properties;
        this.generator = generator;
        this.uploader = uploader;
        this.random = random;
    }

    @Scheduled(fixedRateString = "${flut.trader.interval:2m}", initialDelayString = "${flut.trader.initial-delay:5s}")
    public void deliver() {
        final int count = random.nextInt(properties.minFiles(), properties.maxFiles() + 1);
        final List<GeneratedFile> files = IntStream.rangeClosed(1, count).mapToObj(generator::generate).toList();
        try {
            uploader.upload(files);
            LOG.info("Delivered {} file(s): {}", count, files.stream().map(GeneratedFile::name).toList());
        } catch (final RuntimeException e) {
            LOG.warn("Could not deliver {} file(s), next delivery in {}: {}", count, properties.interval(), e.toString());
        }
    }
}
