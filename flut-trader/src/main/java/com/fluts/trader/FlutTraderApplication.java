package com.fluts.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simulates a trader: every two minutes it uploads one to five files with random scenarios to an
 * SFTP server. Feeds the pipeline SFTP → flut-producer → Kafka → flut-consumer → database.
 */
@SpringBootApplication
public class FlutTraderApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FlutTraderApplication.class, args);
    }
}
