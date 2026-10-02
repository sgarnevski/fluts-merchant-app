package com.fluts.app.web;

import com.fluts.app.scenario.ScenarioNotFoundException;
import com.fluts.app.scenario.ScenarioService;
import com.fluts.app.scenario.ScenarioView;
import com.fluts.app.security.CurrentActor;
import com.fluts.app.security.ScenarioScope;
import com.fluts.domain.Scenario;
import com.fluts.domain.Schuur;
import com.fluts.domain.SchuurResult;
import com.fluts.domain.TradingResult;
import com.fluts.io.TextResultFormatter;
import com.fluts.persistence.ScenarioInputType;
import com.fluts.platform.io.TextScenarioWriter;
import com.fluts.rest.evaluation.EvaluationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD for stored scenarios; results are computed on every read. */
@RestController
@RequestMapping(path = "/api/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Scenarios", description = "Stored scenarios and their results")
public class ScenarioController {

    private static final Set<String> SORTABLE = Set.of("name", "createdAt", "updatedAt");

    private final ScenarioService scenarioService;
    private final CurrentActor currentActor;
    private final TextScenarioWriter textWriter = new TextScenarioWriter();
    private final TextResultFormatter resultFormatter = new TextResultFormatter();

    public ScenarioController(final ScenarioService scenarioService, final CurrentActor currentActor) {
        this.scenarioService = scenarioService;
        this.currentActor = currentActor;
    }

    @GetMapping
    @Operation(summary = "List scenarios",
            description = "Paged; sort by `name`, `createdAt` or `updatedAt`. Each item includes its computed `maxProfit`. "
                    + "Merchant users see their own scenarios; admins see every merchant's and may filter by `merchant`.")
    public ScenarioPage list(
            @Parameter(description = "Only scenarios of this merchant (admins only; others always see their own)") @RequestParam(required = false) final @Nullable String merchant,
            @Parameter(description = "Only scenarios with this input type") @RequestParam(required = false) final @Nullable ScenarioInputType inputType,
            @Parameter(description = "Only scenarios whose name contains this text (case-insensitive)") @RequestParam(required = false) final @Nullable String name,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        pageable.getSort().stream()
                .filter(order -> !SORTABLE.contains(order.getProperty()))
                .findFirst()
                .ifPresent(order -> {
                    throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
                            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                    "Cannot sort by '" + order.getProperty() + "'; use one of name, createdAt, updatedAt"), null);
                });
        return ScenarioPage.from(scenarioService.list(scope(), merchant, inputType, name, pageable).map(ScenarioSummary::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a scenario with its result")
    public ScenarioResponse get(@PathVariable final long id) {
        return ScenarioResponse.from(scenarioService.get(scope(), id));
    }

    @GetMapping("/{id}/result")
    @Operation(summary = "Get only the result of a scenario")
    public EvaluationResult result(@PathVariable final long id) {
        final ScenarioView view = scenarioService.get(scope(), id);
        return new EvaluationResult(view.id(), view.scenario().name(), view.result().maxProfit(),
                view.result().flutCounts());
    }

    @GetMapping(path = "/{id}/file", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download a scenario as a text file in the original input format",
            description = "Useful for large scenarios; the file can be evaluated or imported again as it is.")
    public ResponseEntity<String> file(@PathVariable final long id) {
        final ScenarioView view = scenarioService.get(scope(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(view.scenario().name()))
                .body(textWriter.write(List.of(view.scenario())));
    }

    /** A {@code .txt} download named after the scenario, with characters that are safe in every file system. */
    private static String attachment(final String name) {
        return ContentDisposition.attachment()
                .filename(name.replaceAll("[^A-Za-z0-9._-]+", "_") + ".txt")
                .build().toString();
    }

    @GetMapping(path = "/{id}/result/file", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download the result of a scenario as a text file in the original output format")
    public ResponseEntity<String> resultFile(@PathVariable final long id) {
        final ScenarioView view = scenarioService.get(scope(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(view.scenario().name() + "-result"))
                .body(resultFormatter.format(List.of(view.result())));
    }

    @GetMapping(path = "/{id}/schuurs/{position}/file", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download one schuur as a text file in the original input format",
            description = "The schuur (1-based position) as a scenario of its own, so it can be evaluated on its own.")
    public ResponseEntity<String> schuurFile(@PathVariable final long id, @PathVariable final int position) {
        final ScenarioView view = scenarioService.get(scope(), id);
        final Schuur schuur = schuurAt(view, position);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(view.scenario().name() + "-schuur-" + position))
                .body(textWriter.write(List.of(new Scenario(view.scenario().name(), List.of(schuur)))));
    }

    @GetMapping(path = "/{id}/schuurs/{position}/result/file", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download the result of one schuur as a text file in the original output format",
            description = "The best buy at this schuur alone; the block is numbered with the schuur's position (1-based).")
    public ResponseEntity<String> schuurResultFile(@PathVariable final long id, @PathVariable final int position) {
        final ScenarioView view = scenarioService.get(scope(), id);
        schuurAt(view, position);
        final SchuurResult result = view.schuurResults().get(position - 1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(view.scenario().name() + "-schuur-" + position + "-result"))
                .body(resultFormatter.format(position, new TradingResult(result.maxProfit(), result.flutCounts())));
    }

    private static Schuur schuurAt(final ScenarioView view, final int position) {
        final List<Schuur> schuurs = view.scenario().schuurs();
        if (position < 1 || position > schuurs.size()) {
            throw new ScenarioNotFoundException.SchuurNotFoundException(view.id(), position);
        }
        return schuurs.get(position - 1);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a scenario", description = "For the calling user's merchant. The `Location` header points to the new scenario.")
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody final ScenarioRequest request) {
        final ScenarioResponse created = ScenarioResponse.from(scenarioService.create(currentActor.get().requireMerchant(), request.toDomain()));
        return ResponseEntity.created(URI.create("/api/scenarios/" + created.id())).body(created);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace a scenario", description = "Replaces name and schuurs; input type and creation time stay.")
    public ScenarioResponse replace(@PathVariable final long id, @Valid @RequestBody final ScenarioRequest request) {
        return ScenarioResponse.from(scenarioService.replace(scope(), id, request.toDomain()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a scenario")
    public void delete(@PathVariable final long id) {
        scenarioService.delete(scope(), id);
    }

    private ScenarioScope scope() {
        return currentActor.get().scope();
    }
}
