package com.tms.service;

import com.tms.dto.request.LrRequest;
import com.tms.dto.response.LrResponse;
import com.tms.entity.LorryReceipt;
import com.tms.enums.LrStatus;
import com.tms.enums.AuditAction;
import com.tms.exception.BadRequestException;
import com.tms.exception.DuplicateResourceException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.LorryReceiptRepository;
import com.tms.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LorryReceiptService {

    private final LorryReceiptRepository lrRepository;
    private final TripRepository tripRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public Page<LrResponse> getAllLrs(int page, int size, String sortBy, String sortDir,
                                      LrStatus status, String origin, String destination) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return lrRepository.findWithFilters(
                status,
                origin == null ? "" : origin,
                destination == null ? "" : destination,
                pageable)
                .map(this::toResponse);
    }

    public LrResponse getLrById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    public LrResponse createLr(LrRequest request) {
        if (lrRepository.existsByLrNumber(request.getLrNumber())) {
            throw new DuplicateResourceException("LorryReceipt", "lrNumber", request.getLrNumber());
        }

        LorryReceipt lr = LorryReceipt.builder()
                .lrNumber(request.getLrNumber())
                .consignor(request.getConsignor())
                .consignee(request.getConsignee())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .material(request.getMaterial())
                .weight(request.getWeight())
                .quantity(request.getQuantity())
                .status(request.getStatus() != null ? request.getStatus() : LrStatus.CREATED)
                .build();

        LorryReceipt saved = lrRepository.save(lr);
        LrResponse response = toResponse(saved);
        auditLogService.log("LorryReceipt", saved.getId().toString(), AuditAction.CREATE, null, response);
        return response;
    }

    @Transactional
    public LrResponse updateLr(UUID id, LrRequest request) {
        LorryReceipt lr = findById(id);
        LrStatus oldStatus = lr.getStatus();

        lrRepository.findByLrNumber(request.getLrNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("LorryReceipt", "lrNumber", request.getLrNumber());
                });

        lr.setLrNumber(request.getLrNumber());
        lr.setConsignor(request.getConsignor());
        lr.setConsignee(request.getConsignee());
        lr.setOrigin(request.getOrigin());
        lr.setDestination(request.getDestination());
        lr.setMaterial(request.getMaterial());
        lr.setWeight(request.getWeight());
        lr.setQuantity(request.getQuantity());
        if (request.getStatus() != null) lr.setStatus(request.getStatus());

        LorryReceipt updated = lrRepository.save(lr);
        LrResponse response = toResponse(updated);
        auditLogService.log("LorryReceipt", id.toString(), AuditAction.UPDATE, oldStatus.name(), response);

        // Send email alert when LR transitions to IN_TRANSIT
        if (request.getStatus() == LrStatus.IN_TRANSIT && oldStatus != LrStatus.IN_TRANSIT) {
            emailService.sendLrDispatchAlert(updated);
        }
        return response;
    }

    @Transactional
    public void deleteLr(UUID id) {
        LorryReceipt lr = findById(id);

        boolean associatedWithTrip = tripRepository.existsByLorryReceiptsId(id);
        if (associatedWithTrip) {
            throw new BadRequestException("Cannot delete LR associated with a trip. Remove it from the trip first.");
        }

        lrRepository.delete(lr);
    }

    public LorryReceipt findById(UUID id) {
        return lrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LorryReceipt", "id", id));
    }

    private LrResponse toResponse(LorryReceipt lr) {
        return LrResponse.builder()
                .id(lr.getId())
                .lrNumber(lr.getLrNumber())
                .consignor(lr.getConsignor())
                .consignee(lr.getConsignee())
                .origin(lr.getOrigin())
                .destination(lr.getDestination())
                .material(lr.getMaterial())
                .weight(lr.getWeight())
                .quantity(lr.getQuantity())
                .status(lr.getStatus())
                .createdAt(lr.getCreatedAt())
                .updatedAt(lr.getUpdatedAt())
                .build();
    }
}

