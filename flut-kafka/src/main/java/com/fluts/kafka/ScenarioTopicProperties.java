package com.fluts.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * The scenario topic and its retention, configurable as {@code flut.kafka.*}.
 *
 * <p>Kafka cannot keep "the last N messages": a topic is trimmed by age and by size per partition.
 * The defaults keep about 1000 scenarios at the trader's usual rate (6 hours) and never more than
 * 10 MB per partition, so the broker cannot fill up.
 *
 * @param topic                 name of the scenario topic
 * @param partitions            number of partitions of both topics
 * @param retention             how long scenario messages are kept
 * @param retentionBytes        upper bound per partition, whatever the message rate (both topics)
 * @param segmentBytes          size of a log segment; old data is deleted a whole segment at a time (both topics)
 * @param segmentTime           a segment is closed after this time even when it is not full
 * @param deadLetterRetention   how long messages that could not be processed are kept for inspection
 * @param deadLetterSegmentTime like {@code segmentTime}, for the dead-letter topic
 * @param pipelineSecret        shared by producer and consumer only; signs every message, so the consumer
 *                              accepts only messages the pipeline sent (see {@link ScenarioMessageCodec})
 */
@ConfigurationProperties("flut.kafka")
public record ScenarioTopicProperties(
        @DefaultValue("flut.scenarios") String topic,
        @DefaultValue("3") int partitions,
        @DefaultValue("6h") Duration retention,
        @DefaultValue("10MB") DataSize retentionBytes,
        @DefaultValue("1MB") DataSize segmentBytes,
        @DefaultValue("10m") Duration segmentTime,
        @DefaultValue("7d") Duration deadLetterRetention,
        @DefaultValue("1h") Duration deadLetterSegmentTime,
        String pipelineSecret) {

    /** Where messages go that the consumer could not process. */
    public String deadLetterTopic() {
        return topic + ".DLT";
    }
}
