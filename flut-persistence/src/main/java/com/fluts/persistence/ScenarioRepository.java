package com.fluts.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Scenarios with their schuurs and boxes (loaded lazily, in batches). */
public interface ScenarioRepository extends JpaRepository<ScenarioEntity, Long>, JpaSpecificationExecutor<ScenarioEntity> {

    /** Whether the Kafka message with this id has already been stored. */
    boolean existsByMessageId(UUID messageId);

    /** A scenario, but only if it belongs to the merchant (another merchant's id looks like a missing one). */
    Optional<ScenarioEntity> findByIdAndMerchantCode(long id, String merchantCode);
}
