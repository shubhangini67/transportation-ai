package com.tms.repository;

import com.tms.entity.LorryReceipt;
import com.tms.enums.LrStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LorryReceiptRepository extends JpaRepository<LorryReceipt, UUID>,
        JpaSpecificationExecutor<LorryReceipt> {

    Optional<LorryReceipt> findByLrNumber(String lrNumber);

    boolean existsByLrNumber(String lrNumber);

    @Query("SELECT lr FROM LorryReceipt lr WHERE " +
            "(:status IS NULL OR lr.status = :status) AND " +
            "(:origin = '' OR LOWER(lr.origin) LIKE LOWER(CONCAT('%', :origin, '%'))) AND " +
            "(:destination = '' OR LOWER(lr.destination) LIKE LOWER(CONCAT('%', :destination, '%')))")
    Page<LorryReceipt> findWithFilters(
            @Param("status") LrStatus status,
            @Param("origin") String origin,
            @Param("destination") String destination,
            Pageable pageable);

    long countByStatus(LrStatus status);
}

