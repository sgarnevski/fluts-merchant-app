package com.fluts.producer;

import com.fluts.kafka.ScenarioTopicConfiguration;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on the inbox polling and wires the shared topic module. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(ProducerProperties.class)
@Import(ScenarioTopicConfiguration.class)
public class ProducerConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
