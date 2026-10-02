package com.fluts.consumer;

import com.fluts.kafka.ScenarioMessage;
import com.fluts.persistence.MerchantRepository;
import com.fluts.persistence.ScenarioEntity;
import com.fluts.persistence.ScenarioRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores received scenarios, each message at most once. */
@Service
public class KafkaScenarioStore {

    private final ScenarioRepository repository;
    private final MerchantRepository merchants;
    private final Clock clock;

    public KafkaScenarioStore(final ScenarioRepository repository, final MerchantRepository merchants, final Clock clock) {
        this.repository = repository;
        this.merchants = merchants;
        this.clock = clock;
    }

    /**
     * Stores the scenario unless this message was stored before (Kafka may deliver a message twice,
     * and a producer may send a file again after a crash).
     *
     * @return whether the scenario was stored now
     * @throws UnknownMerchantException if the message's merchant is not registered
     */
    @Transactional
    public boolean store(final ScenarioMessage message) {
        if (repository.existsByMessageId(message.messageId())) {
            return false;
        }
        if (!merchants.existsById(message.merchantCode())) {
            throw new UnknownMerchantException(message.merchantCode());
        }
        repository.save(ScenarioEntity.fromKafka(message.merchantCode(), message.scenario(), message.messageId(), clock.instant()));
        return true;
    }
}
