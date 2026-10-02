package com.fluts.app.web;

import com.fluts.rest.evaluation.EvaluationResponse;
import com.fluts.rest.evaluation.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code POST /api/scenarios/import}: stores all scenarios of an uploaded file. Answers like
 * {@code /api/evaluations} (same result shape, ids included), with {@code 201 Created}.
 */
@RestController
@Tag(name = "Scenarios")
public class ImportController {

    private final EvaluationService evaluationService;

    public ImportController(final EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping(path = "/api/scenarios/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import scenarios from a file",
            description = "`.txt` in the original format of the specification, or `.json` in the normalized format. "
                    + "Answers the results with the ids of the stored scenarios, in file order.")
    public EvaluationResponse importFile(
            @Parameter(description = "A .txt or .json file") @RequestPart("file") final MultipartFile file)
            throws IOException {
        try (InputStream content = file.getInputStream()) {
            return evaluationService.evaluateFile(Objects.requireNonNullElse(file.getOriginalFilename(), ""), content);
        }
    }
}
