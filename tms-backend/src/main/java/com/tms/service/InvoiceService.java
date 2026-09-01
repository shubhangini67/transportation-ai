package com.tms.service;

import com.tms.dto.request.InvoiceRequest;
import com.tms.dto.response.InvoiceResponse;
import com.tms.entity.*;
import com.tms.enums.ExpenseCategory;
import com.tms.enums.InvoiceStatus;
import com.tms.exception.BadRequestException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.ExpenseRepository;
import com.tms.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final ExpenseRepository expenseRepo;
    private final TripService tripService;
    private static final AtomicLong SEQ = new AtomicLong(1);

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(int page, int size, InvoiceStatus status, LocalDate from, LocalDate to) {
        return invoiceRepo.findWithFilters(status, from, to, PageRequest.of(page, size)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest req) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .clientName(req.getClientName())
                .clientEmail(req.getClientEmail())
                .taxRate(req.getTaxRate() != null ? req.getTaxRate() : new BigDecimal("18.00"))
                .notes(req.getNotes())
                .issuedDate(LocalDate.now())
                .dueDate(req.getDueDate() != null ? req.getDueDate() : LocalDate.now().plusDays(30))
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        if (req.getTripId() != null) {
            Trip trip = tripService.findById(req.getTripId());
            invoice.setTrip(trip);
        }

        if (req.getItems() != null) {
            for (InvoiceRequest.InvoiceItemRequest itemReq : req.getItems()) {
                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .description(itemReq.getDescription())
                        .category(itemReq.getCategory() != null ? ExpenseCategory.valueOf(itemReq.getCategory()) : null)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .amount(itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                        .build();
                invoice.getItems().add(item);
            }
        }

        recalculateTotals(invoice);
        return toResponse(invoiceRepo.save(invoice));
    }

    @Transactional
    public InvoiceResponse generateFromTrip(UUID tripId) {
        Trip trip = tripService.findById(tripId);
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        if (expenses.isEmpty()) throw new BadRequestException("No expenses found for trip " + tripId);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .trip(trip)
                .clientName("Trip " + trip.getVehicle().getVehicleNumber())
                .taxRate(new BigDecimal("18.00"))
                .issuedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (Expense exp : expenses) {
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(exp.getDescription() != null ? exp.getDescription() : exp.getCategory().name())
                    .category(exp.getCategory())
                    .quantity(1)
                    .unitPrice(exp.getAmount())
                    .amount(exp.getAmount())
                    .expenseId(exp.getId())
                    .build();
            invoice.getItems().add(item);
        }

        recalculateTotals(invoice);
        log.info("Invoice generated from trip {} with {} items", tripId, expenses.size());
        return toResponse(invoiceRepo.save(invoice));
    }

    @Transactional
    public InvoiceResponse updateInvoiceStatus(UUID id, InvoiceStatus status) {
        Invoice invoice = findById(id);
        invoice.setStatus(status);
        return toResponse(invoiceRepo.save(invoice));
    }

    @Transactional
    public void deleteInvoice(UUID id) {
        invoiceRepo.delete(findById(id));
    }

    public Invoice findById(UUID id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
    }

    private void recalculateTotals(Invoice invoice) {
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = subtotal.multiply(invoice.getTaxRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(subtotal.add(taxAmount));
    }

    private String generateInvoiceNumber() {
        YearMonth ym = YearMonth.now();
        String prefix = String.format("INV-%d%02d-", ym.getYear(), ym.getMonthValue());
        long seq = SEQ.getAndIncrement();
        String num = prefix + String.format("%04d", seq);
        while (invoiceRepo.existsByInvoiceNumber(num)) {
            seq = SEQ.getAndIncrement();
            num = prefix + String.format("%04d", seq);
        }
        return num;
    }

    InvoiceResponse toResponse(Invoice i) {
        List<InvoiceResponse.InvoiceItemResponse> items = i.getItems() != null
                ? i.getItems().stream().map(it -> InvoiceResponse.InvoiceItemResponse.builder()
                    .id(it.getId()).description(it.getDescription())
                    .category(it.getCategory() != null ? it.getCategory().name() : null)
                    .quantity(it.getQuantity()).unitPrice(it.getUnitPrice())
                    .amount(it.getAmount()).expenseId(it.getExpenseId()).build()).toList()
                : Collections.emptyList();

        return InvoiceResponse.builder()
                .id(i.getId()).invoiceNumber(i.getInvoiceNumber())
                .tripId(i.getTrip() != null ? i.getTrip().getId() : null)
                .tripVehicleNumber(i.getTrip() != null ? i.getTrip().getVehicle().getVehicleNumber() : null)
                .tripDriverName(i.getTrip() != null ? i.getTrip().getDriver().getName() : null)
                .clientName(i.getClientName()).clientEmail(i.getClientEmail())
                .subtotal(i.getSubtotal()).taxRate(i.getTaxRate())
                .taxAmount(i.getTaxAmount()).totalAmount(i.getTotalAmount())
                .status(i.getStatus()).notes(i.getNotes())
                .issuedDate(i.getIssuedDate()).dueDate(i.getDueDate())
                .createdAt(i.getCreatedAt()).items(items).build();
    }
}

