package com.fluts.consumer;

import com.fluts.kafka.ScenarioMessage;
import com.fluts.kafka.ScenarioMessageCodec;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Receives scenario messages. Failures are handled by the error handler in {@link ConsumerConfiguration}. */
@Component
public class ScenarioListener {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioListener.class);

    private final ScenarioMessageCodec codec;
    private final KafkaScenarioStore store;

    public ScenarioListener(final ScenarioMessageCodec codec, final KafkaScenarioStore store) {
        this.codec = codec;
        this.store = store;
    }

    @KafkaListener(topics = "#{@scenarioTopic.name()}")
    public void onMessage(final ConsumerRecord<String, String> record) {
        final ScenarioMessage message = codec.decode(record.key(), record.value());
        final boolean stored = store.store(message);
        LOG.info("{} scenario '{}' of merchant {} (message {})", stored ? "Stored" : "Skipped already stored",
                message.scenario().name(), message.merchantCode(), message.messageId());
    }
}
