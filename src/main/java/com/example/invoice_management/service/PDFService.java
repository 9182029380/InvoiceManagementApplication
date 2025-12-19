package com.example.invoice_management.service;

import com.example.invoice_management.entity.ClientCompany;
import com.example.invoice_management.entity.Invoice;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PDFService {

    public String generateInvoicePDF(Invoice invoice) {
        try {
            String clientName = invoice.getPurchaseOrder().getClientCompany().getCompanyName();
            String sanitizedClient = clientName.replaceAll("[^a-zA-Z0-9]", "_");
            String datePart = invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String poNumber = invoice.getPurchaseOrder().getPoNumber();

            String fileName = sanitizedClient + "_" + datePart + "_" + poNumber + ".pdf";

            String filePath = "invoices/" + fileName;
            new java.io.File("invoices").mkdirs();

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Header
            document.add(new Paragraph("TAX INVOICE")
                    .setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));

            // Our Company Details
            document.add(new Paragraph("\n" + invoice.getOurCompany().getCompanyName())
                    .setFontSize(14).setBold());
            document.add(new Paragraph(invoice.getOurCompany().getAddress()));
            document.add(new Paragraph("PAN: " + invoice.getOurCompany().getPanNumber()));
            document.add(new Paragraph("GST: " + invoice.getOurCompany().getGstNumber()));
            // Add email and phone
            document.add(new Paragraph("Email: " + invoice.getOurCompany().getEmail()));
            document.add(new Paragraph("Phone: " + invoice.getOurCompany().getPhone()));

            // Invoice Details
            document.add(new Paragraph("\nInvoice Number: " + invoice.getInvoiceNumber()).setBold());
            document.add(new Paragraph("Invoice Date: " + invoice.getInvoiceDate()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))));
            document.add(new Paragraph("PO Number: " + invoice.getPurchaseOrder().getPoNumber()));

            // Bill To
            document.add(new Paragraph("\nBILL TO:").setBold());
            ClientCompany client = invoice.getPurchaseOrder().getClientCompany();
            document.add(new Paragraph(client.getCompanyName()));
            document.add(new Paragraph(client.getAddress()));
            document.add(new Paragraph("Email: " + client.getEmail()));
            if (client.getPhone() != null) {
                document.add(new Paragraph("Phone: " + client.getPhone()));
            }
            document.add(new Paragraph("PAN: " + client.getPanNumber()));
            document.add(new Paragraph("GST: " + client.getGstNumber()));

            // Table
            Table table = new Table(4);
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Description");
            table.addHeaderCell("Amount");
            table.addHeaderCell("GST (18%)");
            table.addHeaderCell("Total");

            table.addCell(invoice.getPurchaseOrder().getTrainingDetails());
            table.addCell(String.format("₹%.2f", invoice.getSubtotal()));
            table.addCell(String.format("₹%.2f", invoice.getGstAmount()));
            table.addCell(String.format("₹%.2f", invoice.getTotalAmount()));

            document.add(new Paragraph("\n"));
            document.add(table);

            // Total
            document.add(new Paragraph("\nTotal Amount: ₹" +
                    String.format("%.2f", invoice.getTotalAmount()))
                    .setFontSize(14).setBold().setTextAlignment(TextAlignment.RIGHT));

            // Bank Details
            document.add(new Paragraph("\nBANK DETAILS:").setBold());
            document.add(new Paragraph("Bank: " + invoice.getOurCompany().getBankName()));
            document.add(new Paragraph("Account: " + invoice.getOurCompany().getAccountNumber()));
            document.add(new Paragraph("IFSC: " + invoice.getOurCompany().getIfscCode()));
            // add bank address if present
            if (invoice.getOurCompany().getBankAddress() != null) {
                document.add(new Paragraph("Bank Address: " + invoice.getOurCompany().getBankAddress()));
            }

            document.close();
            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }
    }
}