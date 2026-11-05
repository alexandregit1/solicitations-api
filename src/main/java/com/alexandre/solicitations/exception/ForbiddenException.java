package com.alexandre.solicitations.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Authenticated, but not allowed to access this resource (owner or UF coverage). */
public class ForbiddenException extends ResponseStatusException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
