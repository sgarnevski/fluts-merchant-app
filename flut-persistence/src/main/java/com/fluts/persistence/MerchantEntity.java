package com.fluts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A client of the system; every scenario belongs to one. */
@Entity
@Table(name = "merchant")
public class MerchantEntity {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    protected MerchantEntity() {
        // for JPA
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
