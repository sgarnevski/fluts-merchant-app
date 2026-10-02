package com.fluts.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** The registered merchants, by code. */
public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}
