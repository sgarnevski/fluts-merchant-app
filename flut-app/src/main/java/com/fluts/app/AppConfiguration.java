package com.fluts.app;

import com.fluts.persistence.PersistenceConfiguration;
import com.fluts.rest.web.EvaluationWebConfiguration;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Adds the two shared building blocks: the evaluation web layer (its {@code ScenarioRecorder} here
 * is the {@code JpaScenarioRecorder}, so every evaluated scenario is saved) and the persistence module.
 */
@Configuration(proxyBeanMethods = false)
@Import({EvaluationWebConfiguration.class, PersistenceConfiguration.class})
public class AppConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
