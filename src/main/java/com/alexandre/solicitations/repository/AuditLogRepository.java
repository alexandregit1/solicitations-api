package com.alexandre.solicitations.repository;

import com.alexandre.solicitations.model.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByResourceIdOrderByIdAsc(UUID resourceId);
}
