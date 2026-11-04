package com.alexandre.solicitations.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The request is well-formed but breaks a business rule. */
public class BusinessException extends ResponseStatusException {

    public BusinessException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
