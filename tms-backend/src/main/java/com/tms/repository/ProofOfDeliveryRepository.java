package com.tms.repository;

import com.tms.entity.ProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, UUID> {
    List<ProofOfDelivery> findByTripIdOrderByDeliveredAtDesc(UUID tripId);
}
