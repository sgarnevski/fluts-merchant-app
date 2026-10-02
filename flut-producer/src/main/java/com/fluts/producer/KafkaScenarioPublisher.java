package com.fluts.producer;

import com.fluts.kafka.ScenarioMessage;
import com.fluts.kafka.ScenarioMessageCodec;
import com.fluts.kafka.ScenarioTopicProperties;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

/** Sends scenario messages to the scenario topic and waits for the broker to confirm them. */
@Component
public class KafkaScenarioPublisher implements ScenarioPublisher {

    private final KafkaOperations<String, String> kafka;
    private final ScenarioMessageCodec codec;
    private final String topic;
    private final long sendTimeoutMillis;

    public KafkaScenarioPublisher(final KafkaOperations<String, String> kafka, final ScenarioMessageCodec codec,
            final ScenarioTopicProperties topics, final ProducerProperties properties) {
        this.kafka = kafka;
        this.codec = codec;
        this.topic = topics.topic();
        this.sendTimeoutMillis = properties.sendTimeout().toMillis();
    }

    @Override
    public void publish(final List<ScenarioMessage> messages) {
        try {
            final CompletableFuture<?>[] sends = messages.stream()
                    .map(message -> kafka.send(topic, codec.key(message), codec.value(message)))
                    .toArray(CompletableFuture<?>[]::new);
            CompletableFuture.allOf(sends).orTimeout(sendTimeoutMillis, TimeUnit.MILLISECONDS).join();
        } catch (final KafkaException | CompletionException e) {
            throw new PublishException("Kafka did not confirm all " + messages.size() + " messages", e);
        }
    }
}
