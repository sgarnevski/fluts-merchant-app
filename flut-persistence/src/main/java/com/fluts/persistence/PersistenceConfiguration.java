package com.fluts.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Everything an app needs to store scenarios: import this configuration. The Flyway migrations
 * ({@code db/migration}) come with the module; Hibernate only validates its mapping against them.
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = ScenarioEntity.class)
@EnableJpaRepositories(basePackageClasses = ScenarioRepository.class)
@PropertySource("classpath:flut-persistence.properties")
public class PersistenceConfiguration {
}
