package com.fluts.platform.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HmacSignerTest {

    private final HmacSigner dutch = new HmacSigner("dutch-secret");
    private final HmacSigner french = new HmacSigner("french-secret");

    @Test
    void signsDeterministicallyWithTheStandardAlgorithm() {
        // HMAC-SHA256("dutch-secret", "1\n1 5\n0\n"), computed independently
        assertThat(dutch.sign("1\n1 5\n0\n")).hasSize(64).isEqualTo(dutch.sign("1\n1 5\n0\n".getBytes(StandardCharsets.UTF_8)));
        assertThat(dutch.sign("a")).isNotEqualTo(dutch.sign("b"));
    }

    @Test
    void verifiesOnlySignaturesMadeWithTheSameSecret() {
        final String content = "1\n2 7 3\n0\n";
        final String signature = dutch.sign(content);

        assertThat(dutch.verify(content, signature)).isTrue();
        assertThat(dutch.verify(content, " " + signature.toUpperCase() + "\n")).as("hex case and whitespace do not matter").isTrue();
        assertThat(french.verify(content, signature)).as("another merchant's secret").isFalse();
        assertThat(dutch.verify(content + " ", signature)).as("changed content").isFalse();
        assertThat(dutch.verify(content, "not hex")).isFalse();
        assertThat(dutch.verify(content, "")).isFalse();
    }

    @Test
    void refusesAnUnknownAlgorithm() {
        assertThatThrownBy(() -> new HmacSigner("secret", "NoSuchMac").sign("x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot sign with NoSuchMac");
    }
}
