package com.fluts.producer;

import java.io.Serial;

/** The messages could not be sent (now); trying again later may work. */
public final class PublishException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PublishException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
