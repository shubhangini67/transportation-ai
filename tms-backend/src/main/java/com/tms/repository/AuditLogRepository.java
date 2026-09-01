package com.tms.repository;

import com.tms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:entityType = '' OR a.entityType = :entityType) AND " +
            "(:entityId = '' OR a.entityId = :entityId) AND " +
            "(:changedBy = '' OR a.changedBy = :changedBy) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findWithFilters(String entityType, String entityId, String changedBy, Pageable pageable);
}

