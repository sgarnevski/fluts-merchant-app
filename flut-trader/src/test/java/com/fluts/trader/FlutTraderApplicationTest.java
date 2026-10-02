package com.fluts.trader;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

/** Starts the real app through {@code main()}; no SFTP server is there, which it must survive. */
class FlutTraderApplicationTest {

    @Test
    void mainStartsTheApplication() {
        assertThatNoException().isThrownBy(() -> FlutTraderApplication.main(new String[] {
            "--flut.trader.sftp.port=1", "--flut.trader.interval=1h", "--flut.trader.initial-delay=0s",
            "--flut.trader.merchant=dutch", "--flut.trader.secret=dutch-secret"}));
    }
}
