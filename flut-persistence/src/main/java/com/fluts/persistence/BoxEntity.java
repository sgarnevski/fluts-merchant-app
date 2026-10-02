package com.fluts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** One box of a schuur. */
@Entity
@Table(name = "box")
class BoxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "box_seq")
    @SequenceGenerator(name = "box_seq", sequenceName = "box_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schuur_id", nullable = false)
    private SchuurEntity schuur;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private int price;

    protected BoxEntity() {
        // for JPA
    }

    BoxEntity(final SchuurEntity schuur, final int position, final int price) {
        this.schuur = schuur;
        this.position = position;
        this.price = price;
    }

    int getPrice() {
        return price;
    }
}
