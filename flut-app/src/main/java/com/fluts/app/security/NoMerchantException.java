package com.fluts.app.security;

import java.io.Serial;

/** The user is not assigned to a merchant, so there is nothing they may see or create. */
public final class NoMerchantException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoMerchantException(final String username) {
        super("User '" + username + "' is not assigned to a merchant");
    }
}
