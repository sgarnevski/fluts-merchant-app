package com.fluts.app.security;

import org.jspecify.annotations.Nullable;

/**
 * Who is calling: the user and the merchant they act for.
 *
 * @param username     user name
 * @param merchantCode the user's merchant (claim {@code merchant}); null for users without one, e.g. admins
 * @param admin        whether the user may see every merchant
 */
public record Actor(String username, @Nullable String merchantCode, boolean admin) {

    /** The scenarios this user may see: all of them, or one merchant's. */
    public ScenarioScope scope() {
        if (admin) {
            return ScenarioScope.ALL;
        }
        return ScenarioScope.ofMerchant(requireMerchant());
    }

    /** The merchant new scenarios of this user belong to. */
    public String requireMerchant() {
        if (merchantCode == null) {
            throw new NoMerchantException(username);
        }
        return merchantCode;
    }
}
