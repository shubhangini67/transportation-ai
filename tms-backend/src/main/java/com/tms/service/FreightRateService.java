package com.tms.service;

import com.tms.dto.response.FreightRateCardResponse;
import com.tms.entity.FreightRateCard;
import com.tms.entity.Route;
import com.tms.enums.VehicleType;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.FreightRateCardRepository;
import com.tms.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FreightRateService {

    private final FreightRateCardRepository rateCardRepository;
    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public List<FreightRateCardResponse> list() {
        return rateCardRepository.findByActiveTrueOrderByOriginAsc().stream()
                .map(c -> toResponse(c, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public FreightRateCardResponse quote(Long routeId, VehicleType vehicleType) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));
        FreightRateCard card = rateCardRepository
                .findFirstByOriginIgnoreCaseAndDestinationIgnoreCaseAndVehicleTypeAndActiveTrue(
                        route.getOrigin(), route.getDestination(), vehicleType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rate card", "lane", route.getOrigin() + " → " + route.getDestination() + " / " + vehicleType));
        return toResponse(card, route.getDistance());
    }

    private FreightRateCardResponse toResponse(FreightRateCard card, Double distanceKm) {
        BigDecimal subtotal = null;
        BigDecimal gst = null;
        BigDecimal total = null;
        if (distanceKm != null) {
            BigDecimal distanceCharge = card.getRatePerKm().multiply(BigDecimal.valueOf(distanceKm))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = distanceCharge.max(card.getMinCharge());
            gst = subtotal.multiply(card.getGstPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            total = subtotal.add(gst);
        }
        return FreightRateCardResponse.builder()
                .id(card.getId())
                .origin(card.getOrigin())
                .destination(card.getDestination())
                .vehicleType(card.getVehicleType())
                .ratePerKm(card.getRatePerKm())
                .minCharge(card.getMinCharge())
                .gstPercent(card.getGstPercent())
                .active(card.getActive())
                .distanceKm(distanceKm)
                .quotedSubtotal(subtotal)
                .quotedGst(gst)
                .quotedTotal(total)
                .build();
    }
}
