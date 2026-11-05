package com.alexandre.solicitations.exception;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 422 with "missingSteps": ["address"] and "errors": { "confirmation.preferredDate": "..." }. */
public class IncompleteSolicitationException extends ErrorResponseException {

    public IncompleteSolicitationException(List<String> missingSteps, Map<String, String> errors) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, problem(missingSteps, errors), null);
    }

    private static ProblemDetail problem(List<String> missingSteps, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, "The solicitation is incomplete and cannot be submitted");
        problem.setProperty("missingSteps", missingSteps);
        problem.setProperty("errors", errors);
        return problem;
    }
}
