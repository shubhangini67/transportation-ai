package com.tms.service;

import com.tms.dto.request.ProofOfDeliveryRequest;
import com.tms.dto.response.ProofOfDeliveryResponse;
import com.tms.entity.LorryReceipt;
import com.tms.entity.ProofOfDelivery;
import com.tms.entity.Trip;
import com.tms.enums.LrStatus;
import com.tms.enums.UserRole;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.LorryReceiptRepository;
import com.tms.repository.ProofOfDeliveryRepository;
import com.tms.repository.TripRepository;
import com.tms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProofOfDeliveryService {

    private final ProofOfDeliveryRepository podRepository;
    private final TripRepository tripRepository;
    private final LorryReceiptRepository lrRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProofOfDeliveryResponse submit(UUID tripId, ProofOfDeliveryRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        assertCanCapture(trip);

        ProofOfDelivery pod = ProofOfDelivery.builder()
                .tripId(tripId)
                .lrId(request.getLrId())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .otp(request.getOtp())
                .notes(request.getNotes())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deliveredAt(LocalDateTime.now())
                .build();
        ProofOfDelivery saved = podRepository.save(pod);

        if (request.getLrId() != null) {
            LorryReceipt lr = lrRepository.findById(request.getLrId())
                    .orElseThrow(() -> new ResourceNotFoundException("LorryReceipt", "id", request.getLrId()));
            lr.setStatus(LrStatus.DELIVERED);
            lrRepository.save(lr);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProofOfDeliveryResponse> forTrip(UUID tripId) {
        return podRepository.findByTripIdOrderByDeliveredAtDesc(tripId).stream().map(this::toResponse).toList();
    }

    private void assertCanCapture(Trip trip) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;
        userRepository.findByUsername(auth.getName()).ifPresent(user -> {
            if (user.getRole() == UserRole.DRIVER) {
                String loginEmail = user.getEmail();
                String driverEmail = trip.getDriver().getEmail();
                if (loginEmail == null || driverEmail == null || !loginEmail.equalsIgnoreCase(driverEmail)) {
                    throw new AccessDeniedException("Drivers can only capture POD on their own trips");
                }
            }
        });
    }

    private ProofOfDeliveryResponse toResponse(ProofOfDelivery p) {
        return ProofOfDeliveryResponse.builder()
                .id(p.getId())
                .tripId(p.getTripId())
                .lrId(p.getLrId())
                .receiverName(p.getReceiverName())
                .receiverPhone(p.getReceiverPhone())
                .notes(p.getNotes())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .deliveredAt(p.getDeliveredAt())
                .build();
    }
}
