package com.tms.repository;

import com.tms.entity.Invoice;
import com.tms.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);
    Page<Invoice> findByTripId(UUID tripId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE " +
            "(:status IS NULL OR i.status = :status) AND " +
            "i.issuedDate >= COALESCE(:from, i.issuedDate) AND " +
            "i.issuedDate <= COALESCE(:to, i.issuedDate) " +
            "ORDER BY i.createdAt DESC")
    Page<Invoice> findWithFilters(@Param("status") InvoiceStatus status,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  Pageable pageable);
}

