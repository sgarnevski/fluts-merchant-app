package com.fluts.app.web;

import com.fluts.app.scenario.ScenarioNotFoundException;
import com.fluts.app.security.NoMerchantException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Scenario API errors as problem details; runs before the shared evaluation error handler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ScenarioExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail handleNotFound(final ScenarioNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail handleNoMerchant(final NoMerchantException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    /** Lists every invalid field, e.g. {@code "schuurs[0].boxPrices[1]: must be greater than 0"}. */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleInvalidRequest(final MethodArgumentNotValidException exception) {
        final List<String> errors = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request content");
        problem.setTitle("Invalid input");
        problem.setProperty("errors", errors);
        return problem;
    }
}
