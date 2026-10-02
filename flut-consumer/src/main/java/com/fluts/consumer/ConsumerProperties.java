package com.fluts.consumer;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How failures are retried, configurable as {@code flut.consumer.*}.
 *
 * @param retries       how often a message is tried again after a failure that may pass (e.g. database down)
 * @param retryInterval the pause between two tries
 */
@ConfigurationProperties("flut.consumer")
public record ConsumerProperties(@DefaultValue("3") int retries, @DefaultValue("2s") Duration retryInterval) {
}
