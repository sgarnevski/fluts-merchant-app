package com.fluts.platform.signing;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs and verifies content with a shared secret (HMAC-SHA256, hex encoded). Whoever holds the
 * secret can sign; a signature that verifies proves the content came from a holder of the secret
 * and was not changed on the way.
 */
public final class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;
    private final String algorithm;

    public HmacSigner(final String secret) {
        this(secret, ALGORITHM);
    }

    HmacSigner(final String secret, final String algorithm) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
        this.algorithm = algorithm;
    }

    public String sign(final byte[] content) {
        return HexFormat.of().formatHex(mac().doFinal(content));
    }

    public String sign(final String content) {
        return sign(content.getBytes(StandardCharsets.UTF_8));
    }

    /** Whether {@code signature} is this secret's signature of {@code content}; compares in constant time. */
    public boolean verify(final byte[] content, final String signature) {
        try {
            return MessageDigest.isEqual(mac().doFinal(content), HexFormat.of().parseHex(signature.strip()));
        } catch (final IllegalArgumentException notHex) {
            return false;
        }
    }

    public boolean verify(final String content, final String signature) {
        return verify(content.getBytes(StandardCharsets.UTF_8), signature);
    }

    private Mac mac() {
        try {
            final Mac mac = Mac.getInstance(algorithm);
            mac.init(key);
            return mac;
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Cannot sign with " + algorithm, e);
        }
    }
}
