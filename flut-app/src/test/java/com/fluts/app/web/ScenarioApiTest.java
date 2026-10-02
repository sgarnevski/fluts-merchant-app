package com.fluts.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fluts.app.EditorRequestsTestConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The scenario API end to end: HTTP, validation, service, JPA and PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(EditorRequestsTestConfiguration.class)
class ScenarioApiTest {

    private static final String EXAMPLE_2 = """
            {"name": "Example 2", "schuurs": [
              {"boxPrices": [7, 3, 11, 9, 10]},
              {"boxPrices": [1, 2, 3, 4, 10, 16, 10, 4, 16]}]}""";

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;

    ScenarioApiTest(@Autowired final MockMvcTester mvc, @Autowired final JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void emptyDatabase() {
        jdbc.update("DELETE FROM scenario");
    }

    @Test
    void createsAndReadsAScenarioWithItsComputedResult() {
        final MvcTestResult created = create(EXAMPLE_2);
        assertThat(created).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {"merchant": "dutch", "name": "Example 2", "inputType": "JSON", "maxProfit": 40, "flutCounts": [6, 7, 8, 9, 10, 12, 13],
                         "schuurs": [{"boxPrices": [7, 3, 11, 9, 10], "maxProfit": 10, "flutCounts": [2, 4, 5]},
                                     {"boxPrices": [1, 2, 3, 4, 10, 16, 10, 4, 16], "maxProfit": 30, "flutCounts": [4, 5, 8]}]}""");
        final long id = idOf(created);
        assertThat(created).headers().hasValue("Location", "/api/scenarios/" + id);

        assertThat(mvc.get().uri("/api/scenarios/{id}", id)).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("{\"id\": " + id + ", \"name\": \"Example 2\", \"maxProfit\": 40}");
        assertThat(mvc.get().uri("/api/scenarios/{id}/result", id)).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"id": %d, "scenario": "Example 2", "maxProfit": 40, "flutCounts": [6, 7, 8, 9, 10, 12, 13]}"""
                        .formatted(id));
    }

    @Test
    void downloadsAScenarioAsTextFileInTheOriginalFormat() {
        final long id = idOf(create(EXAMPLE_2));

        assertThat(mvc.get().uri("/api/scenarios/{id}/file", id)).hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .hasHeader("Content-Disposition", "attachment; filename=\"Example_2.txt\"")
                .hasBodyTextEqualTo("2\n5 7 3 11 9 10\n9 1 2 3 4 10 16 10 4 16\n0\n");
        assertThat(mvc.get().uri("/api/scenarios/999999/file")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadsAResultAsTextFileInTheOriginalFormat() {
        final long id = idOf(create(EXAMPLE_2));

        assertThat(mvc.get().uri("/api/scenarios/{id}/result/file", id)).hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .hasHeader("Content-Disposition", "attachment; filename=\"Example_2-result.txt\"")
                .hasBodyTextEqualTo("schuurs 1\nMaximum profit is 40.\nNumber of fluts to buy: 6 7 8 9 10 12 13\n");
        assertThat(mvc.get().uri("/api/scenarios/999999/result/file")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadsOneSchuurAndItsResultAsTextFiles() {
        final long id = idOf(create(EXAMPLE_2));

        assertThat(mvc.get().uri("/api/scenarios/{id}/schuurs/2/file", id)).hasStatusOk()
                .hasHeader("Content-Disposition", "attachment; filename=\"Example_2-schuur-2.txt\"")
                .hasBodyTextEqualTo("1\n9 1 2 3 4 10 16 10 4 16\n0\n");
        assertThat(mvc.get().uri("/api/scenarios/{id}/schuurs/2/result/file", id)).hasStatusOk()
                .hasHeader("Content-Disposition", "attachment; filename=\"Example_2-schuur-2-result.txt\"")
                .hasBodyTextEqualTo("schuurs 2\nMaximum profit is 30.\nNumber of fluts to buy: 4 5 8\n");
        assertThat(mvc.get().uri("/api/scenarios/{id}/schuurs/3/file", id)).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.detail").isEqualTo("Scenario " + id + " has no schuur 3");
        assertThat(mvc.get().uri("/api/scenarios/{id}/schuurs/0/result/file", id)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/scenarios/999999/schuurs/1/file")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void replacesAScenarioAndRecomputesTheResult() {
        final long id = idOf(create(EXAMPLE_2));

        assertThat(mvc.put().uri("/api/scenarios/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Cheaper\", \"schuurs\": [{\"boxPrices\": [1, 1]}]}"))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"id": %d, "name": "Cheaper", "inputType": "JSON", "maxProfit": 18, "flutCounts": [2]}"""
                        .formatted(id));
    }

    @Test
    void deletesAScenario() {
        final long id = idOf(create(EXAMPLE_2));

        assertThat(mvc.delete().uri("/api/scenarios/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/scenarios/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownIdsAreNotFound() {
        assertThat(mvc.get().uri("/api/scenarios/999999")).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.detail").isEqualTo("Scenario 999999 not found");
        assertThat(mvc.get().uri("/api/scenarios/999999/result")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put().uri("/api/scenarios/999999").contentType(MediaType.APPLICATION_JSON).content(EXAMPLE_2))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/scenarios/999999")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidScenariosListingEveryProblem() {
        assertThat(create("{\"name\": \" \", \"schuurs\": [{\"boxPrices\": [3, 0]}, {}]}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").asArray().containsExactly(
                        "name: must not be blank",
                        "schuurs[0].boxPrices[1]: must be greater than 0",
                        "schuurs[1].boxPrices: must not be null");
        assertThat(create("{\"name\": \"No piles\", \"schuurs\": []}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").asArray().containsExactly("schuurs: must not be empty");
    }

    @Test
    void listsScenariosPagedWithFiltersAndSorting() throws IOException {
        create(EXAMPLE_2);
        create("{\"name\": \"100% cheap_one\", \"schuurs\": [{\"boxPrices\": [1]}]}");
        importFile("input.txt", Files.readAllBytes(Path.of("../demo/input.txt")));

        assertThat(mvc.get().uri("/api/scenarios?sort=name,asc&size=2")).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"content": [
                           {"merchant": "dutch", "name": "100% cheap_one", "inputType": "JSON", "schuurCount": 1, "maxProfit": 9},
                           {"name": "Example 2", "inputType": "JSON", "schuurCount": 2, "maxProfit": 40}],
                         "page": {"size": 2, "number": 0, "totalElements": 4, "totalPages": 2}}""");
        assertThat(mvc.get().uri("/api/scenarios?inputType=FILE&sort=name")).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Scenario 1", "Scenario 2");
        assertThat(mvc.get().uri("/api/scenarios").param("name", "0% CHEAP_")).hasStatusOk()
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("100% cheap_one");
        assertThat(mvc.get().uri("/api/scenarios").param("name", "%x")).hasStatusOk()
                .bodyJson().extractingPath("$.content").asArray().isEmpty();
        assertThat(mvc.get().uri("/api/scenarios").param("name", " ")).hasStatusOk()
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(4);
    }

    @Test
    void listsTheMerchants() {
        assertThat(mvc.get().uri("/api/merchants")).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [{"code": "dutch", "name": "Dutch merchant"}, {"code": "french", "name": "French merchant"}]""");
    }

    @Test
    void rejectsSortingByOtherFields() {
        assertThat(mvc.get().uri("/api/scenarios?sort=maxProfit")).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail")
                .isEqualTo("Cannot sort by 'maxProfit'; use one of name, createdAt, updatedAt");
    }

    @Test
    void importStoresEveryScenarioOfAFileAndReturnsResultsWithIds() throws IOException {
        final MvcTestResult imported = importFile("input.json", Files.readAllBytes(Path.of("../demo/input.json")));

        assertThat(imported).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {"results": [{"scenario": "Example 1", "maxProfit": 8}, {"scenario": "Example 2", "maxProfit": 40}]}""");
        assertThat(imported).bodyJson().extractingPath("$.results[*].id").asArray().hasSize(2).doesNotContainNull();
        assertThat(mvc.get().uri("/api/scenarios?inputType=FILE")).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    void invalidImportStoresNothing() {
        assertThat(importFile("bad.txt", "1\n2 5\n".getBytes(StandardCharsets.UTF_8)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.line").isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM scenario", Integer.class)).isZero();
    }

    @Test
    void evaluationsAreStoredAndReturnIds() {
        final MvcTestResult evaluated = mvc.post().uri("/api/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scenarios\": [" + EXAMPLE_2 + "]}")
                .exchange();

        assertThat(evaluated).hasStatusOk().bodyJson().extractingPath("$.results[0].id").isNotNull();
        assertThat(mvc.get().uri("/api/scenarios?inputType=JSON")).bodyJson()
                .extractingPath("$.content[0].name").isEqualTo("Example 2");
    }

    private MvcTestResult create(final String json) {
        return mvc.post().uri("/api/scenarios").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult importFile(final String name, final byte[] content) {
        return mvc.post().uri("/api/scenarios/import")
                .multipart()
                .file(new MockMultipartFile("file", name, MediaType.APPLICATION_OCTET_STREAM_VALUE, content))
                .exchange();
    }

    private static long idOf(final MvcTestResult result) {
        final String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return JsonPath.<Number>read(body, "$.id").longValue();
    }
}
