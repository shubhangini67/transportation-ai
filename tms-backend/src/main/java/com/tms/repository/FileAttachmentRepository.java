package com.tms.repository;

import com.tms.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {
    List<FileAttachment> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
    long countByEntityTypeAndEntityId(String entityType, UUID entityId);
}

