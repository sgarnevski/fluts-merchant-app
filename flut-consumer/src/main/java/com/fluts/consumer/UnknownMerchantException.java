package com.fluts.consumer;

import java.io.Serial;

/** The message names a merchant that is not registered; it can never be stored. */
public final class UnknownMerchantException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnknownMerchantException(final String merchantCode) {
        super("Merchant '" + merchantCode + "' is not registered");
    }
}
