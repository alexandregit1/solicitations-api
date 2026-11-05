package com.alexandre.solicitations.aspect;

import com.alexandre.solicitations.model.AuditLog;
import com.alexandre.solicitations.repository.AuditLogRepository;
import com.alexandre.solicitations.security.AuthenticatedUser;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository repository;
    private final MeterRegistry meterRegistry;

    @AfterReturning("@annotation(audit)")
    public void record(JoinPoint joinPoint, Audit audit) {
        repository.save(new AuditLog(audit.value(), currentUserId(), firstUuid(joinPoint.getArgs())));
        meterRegistry.counter("audit.actions", "action", audit.value()).increment();
    }

    private static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? user.id()
                : null;
    }

    private static UUID firstUuid(Object[] args) {
        return Arrays.stream(args)
                .filter(UUID.class::isInstance)
                .map(UUID.class::cast)
                .findFirst()
                .orElse(null);
    }
}
