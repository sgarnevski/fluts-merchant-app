package com.fluts.app.security;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Which scenarios a call may touch: every merchant's, or one merchant's.
 *
 * @param merchantCode the merchant, or null for all merchants
 */
public record ScenarioScope(@Nullable String merchantCode) {

    public static final ScenarioScope ALL = new ScenarioScope(null);

    public static ScenarioScope ofMerchant(final String merchantCode) {
        return new ScenarioScope(merchantCode);
    }

    public Optional<String> merchant() {
        return Optional.ofNullable(merchantCode);
    }
}
