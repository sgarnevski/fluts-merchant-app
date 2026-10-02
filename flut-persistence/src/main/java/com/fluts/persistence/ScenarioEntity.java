package com.fluts.persistence;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;

/**
 * A stored scenario. Converts to and from the domain {@link Scenario}; entities never leave the
 * persistence package.
 */
@Entity
@Table(name = "scenario")
public class ScenarioEntity {

    static final int BATCH_SIZE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scenario_seq")
    @SequenceGenerator(name = "scenario_seq", sequenceName = "scenario_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** The merchant the scenario belongs to; set on creation, never changed. */
    @Column(name = "merchant_code", nullable = false, updatable = false)
    private String merchantCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, updatable = false)
    private ScenarioInputType inputType;

    /** Id of the Kafka message the scenario came from; null for every other input type. */
    @Column(name = "message_id", updatable = false)
    private @Nullable UUID messageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = BATCH_SIZE)
    private List<SchuurEntity> schuurs = new ArrayList<>();

    protected ScenarioEntity() {
        // for JPA
    }

    public ScenarioEntity(final String merchantCode, final Scenario scenario, final ScenarioInputType inputType, final Instant now) {
        this(merchantCode, scenario, inputType, null, now);
    }

    private ScenarioEntity(final String merchantCode, final Scenario scenario, final ScenarioInputType inputType,
            final @Nullable UUID messageId, final Instant now) {
        this.merchantCode = merchantCode;
        this.inputType = inputType;
        this.messageId = messageId;
        this.createdAt = now;
        replaceWith(scenario, now);
    }

    /** A scenario received as the Kafka message {@code messageId}. */
    public static ScenarioEntity fromKafka(final String merchantCode, final Scenario scenario, final UUID messageId,
            final Instant now) {
        return new ScenarioEntity(merchantCode, scenario, ScenarioInputType.KAFKA, messageId, now);
    }

    /** Replaces name and all schuurs; the input type and creation time stay. */
    public final void replaceWith(final Scenario scenario, final Instant now) {
        this.name = scenario.name();
        this.updatedAt = now;
        schuurs.clear();
        final List<Schuur> newSchuurs = scenario.schuurs();
        IntStream.range(0, newSchuurs.size())
                .mapToObj(index -> new SchuurEntity(this, index, newSchuurs.get(index).boxPrices()))
                .forEach(schuurs::add);
    }

    public Scenario toDomain() {
        return new Scenario(name, schuurs.stream().map(schuur -> new Schuur(schuur.getBoxPrices())).toList());
    }

    public Long getId() {
        return id;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public ScenarioInputType getInputType() {
        return inputType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
