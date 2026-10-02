package com.fluts.app.web;

import com.fluts.persistence.MerchantEntity;

/**
 * A merchant (a client of the system).
 *
 * @param code short identifier, e.g. {@code dutch}
 * @param name display name, e.g. "Dutch merchant"
 */
public record MerchantResponse(String code, String name) {

    static MerchantResponse from(final MerchantEntity merchant) {
        return new MerchantResponse(merchant.getCode(), merchant.getName());
    }
}
