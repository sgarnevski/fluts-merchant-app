package com.fluts.producer;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.io.InputLimits;
import com.fluts.io.JsonScenarioParser;
import com.fluts.kafka.ScenarioMessage;
import com.fluts.kafka.ScenarioMessageCodec;
import com.fluts.kafka.ScenarioTopicProperties;
import com.fluts.platform.signing.HmacSigner;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.unit.DataSize;

class KafkaScenarioPublisherTest {

    private static final ScenarioMessage MESSAGE = new ScenarioMessage(
            UUID.fromString("00000000-0000-0000-0000-000000000007"), "dutch", new Scenario("One", List.of(new Schuur(List.of(4)))));

    @SuppressWarnings("unchecked")
    private final KafkaOperations<String, String> kafka = mock(KafkaOperations.class);
    private final KafkaScenarioPublisher publisher = new KafkaScenarioPublisher(kafka,
            new ScenarioMessageCodec(new JsonScenarioParser(InputLimits.DEFAULT), new HmacSigner("pipeline-secret")),
            new ScenarioTopicProperties("flut.scenarios", 3, Duration.ofHours(6), DataSize.ofMegabytes(10),
                    DataSize.ofMegabytes(1), Duration.ofMinutes(10), Duration.ofDays(7), Duration.ofHours(1), "pipeline-secret"),
            new ProducerProperties(Path.of("unused"), Map.of(), Duration.ofSeconds(5), Duration.ofSeconds(2),
                    Duration.ofMillis(200), DataSize.ofKilobytes(512)));

    @Test
    void sendsEachMessageWithItsIdAsKeyAndReturnsWhenConfirmed() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(List.of(MESSAGE));

        verify(kafka).send(eq("flut.scenarios"), eq("00000000-0000-0000-0000-000000000007"),
                startsWith("{\"merchant\":\"dutch\",\"scenario\":{\"name\":\"One\",\"schuurs\":[{\"boxPrices\":[4]}]},\"signature\":\""));
    }

    @Test
    void publishingNothingSucceeds() {
        assertThatNoException().isThrownBy(() -> publisher.publish(List.of()));
    }

    @Test
    void failsWhenTheBrokerRejectsAMessage() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not enough replicas")));

        assertThatThrownBy(() -> publisher.publish(List.of(MESSAGE)))
                .isInstanceOf(PublishException.class)
                .hasMessage("Kafka did not confirm all 1 messages")
                .hasRootCauseMessage("not enough replicas");
    }

    @Test
    void failsWhenTheBrokerIsNotReachable() {
        when(kafka.send(anyString(), anyString(), any())).thenThrow(new KafkaException("no metadata"));

        assertThatThrownBy(() -> publisher.publish(List.of(MESSAGE))).isInstanceOf(PublishException.class);
    }

    @Test
    void failsWhenTheConfirmationDoesNotArriveInTime() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<SendResult<String, String>>());

        assertThatThrownBy(() -> publisher.publish(List.of(MESSAGE))).isInstanceOf(PublishException.class);
    }
}
