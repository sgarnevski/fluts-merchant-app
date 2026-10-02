package com.fluts.trader;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.platform.io.JsonScenarioWriter;
import com.fluts.platform.io.TextScenarioWriter;
import com.fluts.platform.signing.HmacSigner;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

/**
 * Makes up scenario files: one to three scenarios of one to five schuurs with up to ten boxes
 * priced 1 to 20 florins (so some boxes earn money and some lose it), randomly in the original
 * text format or in the normalized JSON format.
 */
public final class ScenarioFileGenerator {

    static final int MAX_SCENARIOS = 3;
    static final int MAX_SCHUURS = 5;
    static final int MAX_BOXES = 10;
    static final int MAX_PRICE = 20;

    /** A schuur that announces three boxes but lists two prices. */
    private static final String INVALID_CONTENT = "1\n3 5 6\n0\n";

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final DateTimeFormatter NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RandomGenerator random;
    private final Clock clock;
    private final String merchant;
    private final HmacSigner signer;
    private final double invalidFileRatio;
    private final TextScenarioWriter textWriter = new TextScenarioWriter();
    private final JsonScenarioWriter jsonWriter = new JsonScenarioWriter();

    /**
     * @param merchant         the merchant the files are made for; part of file and scenario names
     * @param signer           signs every file with the merchant's secret
     * @param invalidFileRatio share of deliberately broken files, 0 to 1
     */
    public ScenarioFileGenerator(final RandomGenerator random, final Clock clock, final String merchant,
            final HmacSigner signer, final double invalidFileRatio) {
        this.random = random;
        this.clock = clock;
        this.merchant = merchant;
        this.signer = signer;
        this.invalidFileRatio = invalidFileRatio;
    }

    /**
     * @param sequence number of the file within its delivery; part of the file name, which is unique
     *                 together with the current time
     */
    public GeneratedFile generate(final int sequence) {
        final ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
        final String baseName = merchant + "-" + FILE_TIMESTAMP.format(now) + "-" + sequence;
        if (random.nextDouble() < invalidFileRatio) {
            return signed(baseName + ".txt", INVALID_CONTENT);
        }
        final String label = capitalize(merchant) + " delivery " + NAME_TIMESTAMP.format(now) + " #" + sequence;
        final List<Scenario> scenarios = IntStream.rangeClosed(1, random.nextInt(1, MAX_SCENARIOS + 1))
                .mapToObj(position -> new Scenario(label + "." + position, randomSchuurs()))
                .toList();
        return random.nextBoolean()
                ? signed(baseName + ".json", jsonWriter.write(scenarios))
                : signed(baseName + ".txt", textWriter.write(scenarios));
    }

    private GeneratedFile signed(final String name, final String content) {
        return new GeneratedFile(name, content, signer.sign(content));
    }

    private static String capitalize(final String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    private List<Schuur> randomSchuurs() {
        return IntStream.range(0, random.nextInt(1, MAX_SCHUURS + 1))
                .mapToObj(schuur -> new Schuur(IntStream.range(0, random.nextInt(0, MAX_BOXES + 1))
                        .mapToObj(box -> random.nextInt(1, MAX_PRICE + 1))
                        .toList()))
                .toList();
    }
}
