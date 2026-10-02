package com.fluts.kafka;

import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.platform.signing.HmacSigner;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the scenario topic and its dead-letter topic with their retention settings. Spring's
 * {@code KafkaAdmin} creates them at startup if they do not exist, so whichever app starts first
 * creates them with the same settings. Imported by the producer and the consumer.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ScenarioTopicProperties.class)
public class ScenarioTopicConfiguration {

    @Bean
    NewTopic scenarioTopic(final ScenarioTopicProperties properties) {
        return topic(properties.topic(), properties, properties.retention(), properties.segmentTime());
    }

    @Bean
    NewTopic scenarioDeadLetterTopic(final ScenarioTopicProperties properties) {
        return topic(properties.deadLetterTopic(), properties, properties.deadLetterRetention(),
                properties.deadLetterSegmentTime());
    }

    @Bean
    ScenarioMessageCodec scenarioMessageCodec(final ScenarioTopicProperties properties) {
        return new ScenarioMessageCodec(new JsonScenarioParser(InputLimits.DEFAULT), new HmacSigner(properties.pipelineSecret()));
    }

    private static NewTopic topic(final String name, final ScenarioTopicProperties properties,
            final Duration retention, final Duration segmentTime) {
        return TopicBuilder.name(name)
                .partitions(properties.partitions())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retention.toMillis()))
                .config(TopicConfig.RETENTION_BYTES_CONFIG, String.valueOf(properties.retentionBytes().toBytes()))
                .config(TopicConfig.SEGMENT_BYTES_CONFIG, String.valueOf(properties.segmentBytes().toBytes()))
                .config(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(segmentTime.toMillis()))
                .build();
    }
}
