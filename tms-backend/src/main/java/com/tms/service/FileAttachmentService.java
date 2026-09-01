package com.tms.service;

import com.tms.dto.response.FileAttachmentResponse;
import com.tms.entity.FileAttachment;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileAttachmentService {

    private final FileAttachmentRepository attachmentRepo;
    private final FileStorageService fileStorageService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Transactional
    public FileAttachmentResponse upload(MultipartFile file, String entityType, UUID entityId) {
        String storagePath = fileStorageService.storeFile(file);
        FileAttachment attachment = FileAttachment.builder()
                .entityType(entityType.toUpperCase())
                .entityId(entityId)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .build();
        return toResponse(attachmentRepo.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> listByEntity(String entityType, UUID entityId) {
        return attachmentRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType.toUpperCase(), entityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Resource download(UUID attachmentId) {
        FileAttachment att = findById(attachmentId);
        try {
            String filename = att.getStoragePath().replace("/uploads/", "");
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) return resource;
            throw new ResourceNotFoundException("File", "path", filePath.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Transactional
    public void delete(UUID attachmentId) {
        FileAttachment att = findById(attachmentId);
        try {
            String filename = att.getStoragePath().replace("/uploads/", "");
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", e.getMessage());
        }
        attachmentRepo.delete(att);
    }

    public FileAttachment findById(UUID id) {
        return attachmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FileAttachment", "id", id));
    }

    private FileAttachmentResponse toResponse(FileAttachment a) {
        return FileAttachmentResponse.builder()
                .id(a.getId()).entityType(a.getEntityType()).entityId(a.getEntityId())
                .fileName(a.getFileName()).fileType(a.getFileType()).fileSize(a.getFileSize())
                .storagePath(a.getStoragePath()).createdAt(a.getCreatedAt()).createdBy(a.getCreatedBy())
                .build();
    }
}

