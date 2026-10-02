package com.fluts.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ScenarioTopicConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ScenarioTopicConfiguration.class)
            .withPropertyValues("flut.kafka.pipeline-secret=test-secret");

    @Test
    void declaresTheScenarioTopicWithBoundedRetention() {
        runner.run(context -> {
            final NewTopic topic = context.getBean("scenarioTopic", NewTopic.class);

            assertThat(topic.name()).isEqualTo("flut.scenarios");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.configs())
                    .containsEntry("cleanup.policy", "delete")
                    .containsEntry("retention.ms", "21600000")
                    .containsEntry("retention.bytes", "10485760")
                    .containsEntry("segment.bytes", "1048576")
                    .containsEntry("segment.ms", "600000");
        });
    }

    @Test
    void declaresTheDeadLetterTopicWithLongerRetention() {
        runner.run(context -> {
            final NewTopic topic = context.getBean("scenarioDeadLetterTopic", NewTopic.class);

            assertThat(topic.name()).isEqualTo("flut.scenarios.DLT");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.configs())
                    .containsEntry("retention.ms", "604800000")
                    .containsEntry("retention.bytes", "10485760")
                    .containsEntry("segment.ms", "3600000");
        });
    }

    @Test
    void needsThePipelineSecret() {
        new ApplicationContextRunner().withUserConfiguration(ScenarioTopicConfiguration.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void retentionIsConfigurable() {
        runner.withPropertyValues("flut.kafka.topic=other", "flut.kafka.partitions=1", "flut.kafka.retention=30m",
                "flut.kafka.retention-bytes=2MB").run(context -> {
                    final NewTopic topic = context.getBean("scenarioTopic", NewTopic.class);

                    assertThat(topic.name()).isEqualTo("other");
                    assertThat(topic.numPartitions()).isEqualTo(1);
                    assertThat(topic.configs()).containsEntry("retention.ms", "1800000").containsEntry("retention.bytes", "2097152");
                    assertThat(context.getBean(ScenarioTopicProperties.class).deadLetterTopic()).isEqualTo("other.DLT");
                    assertThat(context.getBean(ScenarioMessageCodec.class)).isNotNull();
                });
    }
}
