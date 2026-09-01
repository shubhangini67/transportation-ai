package com.tms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.entity.AuditLog;
import com.tms.enums.AuditAction;
import com.tms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void log(String entityType, String entityId, AuditAction action, Object oldVal, Object newVal) {
        try {
            String user = "system";
            try {
                user = SecurityContextHolder.getContext().getAuthentication().getName();
            } catch (Exception ignored) {}

            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .changedBy(user)
                    .timestamp(LocalDateTime.now())
                    .oldValue(oldVal != null ? objectMapper.writeValueAsString(oldVal) : null)
                    .newValue(newVal != null ? objectMapper.writeValueAsString(newVal) : null)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogs(String entityType, String entityId, String changedBy, int page, int size) {
        return auditLogRepository.findWithFilters(
                entityType == null ? "" : entityType,
                entityId == null ? "" : entityId,
                changedBy == null ? "" : changedBy,
                PageRequest.of(page, size));
    }
}

