package com.fluts.producer;

import com.fluts.kafka.ScenarioMessage;
import java.util.List;

/** Port for sending scenario messages. */
public interface ScenarioPublisher {

    /**
     * Sends the messages and returns once all of them are confirmed.
     *
     * @throws PublishException if not all messages were confirmed; sending them again later is safe
     */
    void publish(List<ScenarioMessage> messages);
}
