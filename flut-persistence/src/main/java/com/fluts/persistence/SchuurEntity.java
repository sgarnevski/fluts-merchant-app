package com.fluts.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.hibernate.annotations.BatchSize;

/** One schuur (pile) of a scenario; its boxes are ordered top to bottom by {@code position}. */
@Entity
@Table(name = "schuur")
class SchuurEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "schuur_seq")
    @SequenceGenerator(name = "schuur_seq", sequenceName = "schuur_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioEntity scenario;

    @Column(nullable = false)
    private int position;

    /** Loaded in batches for many schuurs at once, so a page of scenarios needs a fixed number of queries. */
    @OneToMany(mappedBy = "schuur", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = ScenarioEntity.BATCH_SIZE)
    private List<BoxEntity> boxes = new ArrayList<>();

    protected SchuurEntity() {
        // for JPA
    }

    SchuurEntity(final ScenarioEntity scenario, final int position, final List<Integer> boxPrices) {
        this.scenario = scenario;
        this.position = position;
        IntStream.range(0, boxPrices.size())
                .mapToObj(index -> new BoxEntity(this, index, boxPrices.get(index)))
                .forEach(boxes::add);
    }

    List<Integer> getBoxPrices() {
        return boxes.stream().map(BoxEntity::getPrice).toList();
    }
}
