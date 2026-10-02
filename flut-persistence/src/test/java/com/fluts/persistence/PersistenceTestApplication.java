package com.fluts.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/** A minimal app around the persistence module, so its tests have a Spring Boot configuration to start. */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(PersistenceConfiguration.class)
class PersistenceTestApplication {
}
