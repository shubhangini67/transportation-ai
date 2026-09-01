package com.tms.service;

import com.tms.entity.LorryReceipt;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class LrPdfService {

    public byte[] generatePdf(LorryReceipt lr) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph title = new Paragraph("LORRY RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            // LR Number
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            doc.add(new Paragraph("LR Number: " + lr.getLrNumber(), headerFont));
            doc.add(new Paragraph("Status: " + lr.getStatus().name(), normalFont));
            doc.add(new Paragraph(" "));

            // Details table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            addRow(table, "Consignor", lr.getConsignor(), headerFont, normalFont);
            addRow(table, "Consignee", lr.getConsignee(), headerFont, normalFont);
            addRow(table, "Origin", lr.getOrigin(), headerFont, normalFont);
            addRow(table, "Destination", lr.getDestination(), headerFont, normalFont);
            addRow(table, "Material", lr.getMaterial() != null ? lr.getMaterial() : "N/A", headerFont, normalFont);
            addRow(table, "Weight (kg)", String.valueOf(lr.getWeight()), headerFont, normalFont);
            addRow(table, "Quantity", String.valueOf(lr.getQuantity()), headerFont, normalFont);
            addRow(table, "Created At", lr.getCreatedAt() != null ? lr.getCreatedAt().toString() : "—", headerFont, normalFont);
            doc.add(table);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("This is a computer-generated document.", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate LR PDF: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(new Color(245, 245, 245));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }
}

