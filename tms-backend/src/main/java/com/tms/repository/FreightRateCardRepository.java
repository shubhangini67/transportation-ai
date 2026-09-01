package com.tms.repository;

import com.tms.entity.FreightRateCard;
import com.tms.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreightRateCardRepository extends JpaRepository<FreightRateCard, UUID> {
    List<FreightRateCard> findByActiveTrueOrderByOriginAsc();

    Optional<FreightRateCard> findFirstByOriginIgnoreCaseAndDestinationIgnoreCaseAndVehicleTypeAndActiveTrue(
            String origin, String destination, VehicleType vehicleType);
}
