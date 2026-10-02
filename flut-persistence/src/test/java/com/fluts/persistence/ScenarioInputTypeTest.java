package com.fluts.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fluts.io.InputType;
import org.junit.jupiter.api.Test;

class ScenarioInputTypeTest {

    @Test
    void coversEveryInputTypeOfTheCoreAndAddsKafka() {
        assertThat(ScenarioInputType.of(InputType.JSON)).isEqualTo(ScenarioInputType.JSON);
        assertThat(ScenarioInputType.of(InputType.FILE)).isEqualTo(ScenarioInputType.FILE);
        assertThat(ScenarioInputType.values()).extracting(ScenarioInputType::name).containsExactly("JSON", "FILE", "KAFKA");
    }
}
