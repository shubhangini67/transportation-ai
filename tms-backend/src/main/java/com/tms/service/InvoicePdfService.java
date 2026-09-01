package com.tms.service;

import com.tms.entity.Invoice;
import com.tms.entity.InvoiceItem;
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
public class InvoicePdfService {

    public byte[] generatePdf(Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // Title
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            // Invoice info
            doc.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), headerFont));
            doc.add(new Paragraph("Status: " + invoice.getStatus().name(), normalFont));
            doc.add(new Paragraph("Issued: " + (invoice.getIssuedDate() != null ? invoice.getIssuedDate().toString() : "—"), normalFont));
            doc.add(new Paragraph("Due: " + (invoice.getDueDate() != null ? invoice.getDueDate().toString() : "—"), normalFont));
            doc.add(new Paragraph(" "));

            // Client info
            doc.add(new Paragraph("Bill To:", headerFont));
            doc.add(new Paragraph(invoice.getClientName(), normalFont));
            if (invoice.getClientEmail() != null) doc.add(new Paragraph(invoice.getClientEmail(), normalFont));
            doc.add(new Paragraph(" "));

            // Items table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{50, 10, 20, 20});

            addHeaderCell(table, "Description", headerFont);
            addHeaderCell(table, "Qty", headerFont);
            addHeaderCell(table, "Unit Price", headerFont);
            addHeaderCell(table, "Amount", headerFont);

            for (InvoiceItem item : invoice.getItems()) {
                addCell(table, item.getDescription(), normalFont);
                addCell(table, String.valueOf(item.getQuantity()), normalFont);
                addCell(table, "₹" + item.getUnitPrice().toPlainString(), normalFont);
                addCell(table, "₹" + item.getAmount().toPlainString(), normalFont);
            }
            doc.add(table);
            doc.add(new Paragraph(" "));

            // Totals
            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(50);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addRow(totals, "Subtotal:", "₹" + invoice.getSubtotal().toPlainString(), headerFont, normalFont);
            addRow(totals, "Tax (" + invoice.getTaxRate().toPlainString() + "%):", "₹" + invoice.getTaxAmount().toPlainString(), headerFont, normalFont);
            addRow(totals, "TOTAL:", "₹" + invoice.getTotalAmount().toPlainString(), headerFont, headerFont);
            doc.add(totals);

            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Notes: " + invoice.getNotes(), normalFont));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("This is a computer-generated invoice.", smallFont));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Invoice PDF generation failed", e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setBorder(0); l.setPadding(4); l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, valueFont));
        v.setBorder(0); v.setPadding(4); v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(v);
    }
}

