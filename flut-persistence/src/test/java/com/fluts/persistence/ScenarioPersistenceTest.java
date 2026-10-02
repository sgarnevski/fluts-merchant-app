package com.fluts.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/** The normalized mapping against the real Flyway schema in PostgreSQL. */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestConfiguration.class)
class ScenarioPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");
    private static final Scenario EXAMPLE = new Scenario("Example 2", List.of(
            new Schuur(List.of(7, 3, 11, 9, 10)),
            new Schuur(List.of()),
            new Schuur(List.of(1, 2, 3, 4, 10, 16, 10, 4, 16))));

    private final ScenarioRepository repository;
    private final MerchantRepository merchants;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    ScenarioPersistenceTest(@Autowired final ScenarioRepository repository, @Autowired final MerchantRepository merchants,
            @Autowired final EntityManager entityManager, @Autowired final JdbcTemplate jdbc) {
        this.repository = repository;
        this.merchants = merchants;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    @Test
    void storesOneRowPerScenarioSchuurAndBoxAndKeepsTheOrder() {
        final long id = repository.save(new ScenarioEntity("dutch", EXAMPLE, ScenarioInputType.FILE, NOW)).getId();
        flushAndClear();

        final ScenarioEntity loaded = repository.findById(id).orElseThrow();

        assertThat(loaded.toDomain()).isEqualTo(EXAMPLE);
        assertThat(loaded.getMerchantCode()).isEqualTo("dutch");
        assertThat(loaded.getInputType()).isEqualTo(ScenarioInputType.FILE);
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(count("schuur")).isEqualTo(3);
        assertThat(count("box")).isEqualTo(14);
    }

    @Test
    void replacesAllSchuursKeepingInputTypeAndCreationTime() {
        final long id = repository.save(new ScenarioEntity("dutch", EXAMPLE, ScenarioInputType.JSON, NOW)).getId();
        flushAndClear();
        final Scenario replacement = new Scenario("Replaced", List.of(new Schuur(List.of(5)), new Schuur(List.of(6, 7))));

        repository.findById(id).orElseThrow().replaceWith(replacement, NOW.plusSeconds(60));
        flushAndClear();

        final ScenarioEntity loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.toDomain()).isEqualTo(replacement);
        assertThat(loaded.getInputType()).isEqualTo(ScenarioInputType.JSON);
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(loaded.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(count("box")).isEqualTo(3);
    }

    @Test
    void deletingAScenarioDeletesItsSchuursAndBoxes() {
        final ScenarioEntity saved = repository.save(new ScenarioEntity("dutch", EXAMPLE, ScenarioInputType.JSON, NOW));
        flushAndClear();

        repository.deleteById(saved.getId());
        flushAndClear();

        assertThat(count("scenario") + count("schuur") + count("box")).isZero();
    }

    @Test
    void loadsAPageOfScenariosWithAFixedNumberOfQueries() {
        repository.saveAll(IntStream.range(0, 10)
                .mapToObj(index -> new ScenarioEntity("dutch", EXAMPLE, ScenarioInputType.JSON, NOW))
                .toList());
        flushAndClear();
        final Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        final List<Scenario> scenarios = repository.findAll(ScenarioSpecifications.matching(null, null, null), PageRequest.of(0, 20))
                .map(ScenarioEntity::toDomain)
                .getContent();

        assertThat(scenarios).hasSize(10).allMatch(EXAMPLE::equals);
        assertThat(statistics.getPrepareStatementCount()).as("page + schuurs batch + boxes batch").isEqualTo(3);
    }

    @Test
    void schemaRejectsNonPositivePrices() {
        final ScenarioEntity saved = repository.save(new ScenarioEntity("dutch", EXAMPLE, ScenarioInputType.JSON, NOW));
        flushAndClear();
        final long schuurId = jdbc.queryForObject("SELECT min(id) FROM schuur WHERE scenario_id = ?", Long.class, saved.getId());

        assertThatThrownBy(() -> jdbc.update("INSERT INTO box (schuur_id, position, price) VALUES (?, 99, 0)", schuurId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void storesKafkaScenariosWithTheirMessageId() {
        final UUID messageId = UUID.randomUUID();

        final long id = repository.save(ScenarioEntity.fromKafka("french", EXAMPLE, messageId, NOW)).getId();
        flushAndClear();

        assertThat(repository.findById(id).orElseThrow().getInputType()).isEqualTo(ScenarioInputType.KAFKA);
        assertThat(repository.findById(id).orElseThrow().getMerchantCode()).isEqualTo("french");
        assertThat(repository.existsByMessageId(messageId)).isTrue();
        assertThat(repository.existsByMessageId(UUID.randomUUID())).isFalse();
    }

    @Test
    void schemaRejectsASecondScenarioForTheSameMessage() {
        final UUID messageId = UUID.randomUUID();
        repository.saveAndFlush(ScenarioEntity.fromKafka("french", EXAMPLE, messageId, NOW));

        assertThatThrownBy(() -> repository.saveAndFlush(ScenarioEntity.fromKafka("french", EXAMPLE, messageId, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtersByInputTypeAndByNameIgnoringCaseWithoutWildcards() {
        repository.save(new ScenarioEntity("dutch", new Scenario("100% cheap_one", EXAMPLE.schuurs()), ScenarioInputType.JSON, NOW));
        repository.save(new ScenarioEntity("french", new Scenario("Imported", EXAMPLE.schuurs()), ScenarioInputType.FILE, NOW));
        flushAndClear();

        assertThat(namesMatching(ScenarioInputType.FILE, null)).containsExactly("Imported");
        assertThat(namesMatching(null, "0% CHEAP_")).containsExactly("100% cheap_one");
        assertThat(namesMatching(null, "%x")).isEmpty();
        assertThat(namesMatching(null, "_")).containsExactly("100% cheap_one");
        assertThat(namesMatching(null, " ")).hasSize(2);
        assertThat(namesMatching(ScenarioInputType.KAFKA, "imp")).isEmpty();
    }

    @Test
    void merchantsAreRegisteredAndScenariosBelongToOne() {
        assertThat(merchants.findAll()).extracting(MerchantEntity::getCode, MerchantEntity::getName)
                .containsExactlyInAnyOrder(tuple("dutch", "Dutch merchant"), tuple("french", "French merchant"));
        final long dutchId = repository.save(new ScenarioEntity("dutch", EXAMPLE, ScenarioInputType.JSON, NOW)).getId();
        flushAndClear();

        assertThat(repository.findByIdAndMerchantCode(dutchId, "dutch")).isPresent();
        assertThat(repository.findByIdAndMerchantCode(dutchId, "french")).as("another merchant's scenario looks missing").isEmpty();
        assertThat(repository.findAll(ScenarioSpecifications.matching("french", null, null))).isEmpty();
        assertThatThrownBy(() -> repository.saveAndFlush(new ScenarioEntity("unknown", EXAMPLE, ScenarioInputType.JSON, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private List<String> namesMatching(final ScenarioInputType inputType, final String nameContains) {
        return repository.findAll(ScenarioSpecifications.matching(null, inputType, nameContains)).stream()
                .map(entity -> entity.toDomain().name())
                .toList();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private int count(final String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
