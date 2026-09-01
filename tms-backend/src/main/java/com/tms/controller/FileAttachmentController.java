package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.FileAttachmentResponse;
import com.tms.service.FileAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Tag(name = "File Attachments", description = "Document attachment APIs")
public class FileAttachmentController {

    private final FileAttachmentService attachmentService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Upload a file attachment")
    public ResponseEntity<ApiResponse<FileAttachmentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") UUID entityId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(attachmentService.upload(file, entityType, entityId)));
    }

    @GetMapping
    @Operation(summary = "List attachments for an entity")
    public ResponseEntity<ApiResponse<List<FileAttachmentResponse>>> listByEntity(
            @RequestParam String entityType,
            @RequestParam UUID entityId) {
        return ResponseEntity.ok(ApiResponse.ok(attachmentService.listByEntity(entityType, entityId)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Resource resource = attachmentService.download(id);
        com.tms.entity.FileAttachment att = attachmentService.findById(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(att.getFileType()))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

