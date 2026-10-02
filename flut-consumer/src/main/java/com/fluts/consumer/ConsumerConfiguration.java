package com.fluts.consumer;

import com.fluts.io.ScenarioParseException;
import com.fluts.kafka.ScenarioTopicConfiguration;
import com.fluts.kafka.ScenarioTopicProperties;
import com.fluts.persistence.PersistenceConfiguration;
import java.time.Clock;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Wires the shared topic and persistence modules and decides what happens to messages that fail. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsumerProperties.class)
@Import({ScenarioTopicConfiguration.class, PersistenceConfiguration.class})
public class ConsumerConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * A message that is not a valid signed scenario of a registered merchant can never succeed: it
     * goes straight to the dead-letter topic. Any other failure (e.g. the database is down) is retried a few times first. Either way
     * the consumer then continues, so one bad message does not block the rest.
     */
    @Bean
    DefaultErrorHandler errorHandler(final KafkaOperations<?, ?> kafka, final ScenarioTopicProperties topics,
            final ConsumerProperties properties) {
        final DeadLetterPublishingRecoverer toDeadLetterTopic = new DeadLetterPublishingRecoverer(kafka,
                (record, failure) -> new TopicPartition(topics.deadLetterTopic(), record.partition()));
        final DefaultErrorHandler errorHandler = new DefaultErrorHandler(toDeadLetterTopic,
                new FixedBackOff(properties.retryInterval().toMillis(), properties.retries()));
        errorHandler.addNotRetryableExceptions(ScenarioParseException.class, UnknownMerchantException.class);
        return errorHandler;
    }
}
