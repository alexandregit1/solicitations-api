package com.alexandre.solicitations.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Records a successful call in audit_logs: the action, who did it and the resource (the first UUID argument).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** Action name, e.g. SOLICITATION_SUBMITTED. */
    String value();
}
