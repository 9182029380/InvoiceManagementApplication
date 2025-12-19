package com.example.invoice_management.controller;

import com.example.invoice_management.entity.Invoice;
import com.example.invoice_management.entity.InvoiceStatus;
import com.example.invoice_management.repository.InvoiceRepository;
import com.example.invoice_management.service.EmailService;
import com.example.invoice_management.service.InvoiceService;
import com.example.invoice_management.service.PDFService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final PDFService pdfService;
    private final EmailService emailService;
    private final InvoiceRepository invoiceRepository;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateInvoice(
            @RequestParam String companyId,
            @RequestParam String poNumber) {

        Invoice invoice = invoiceService.generateInvoice(companyId, poNumber);
        String pdfPath = pdfService.generateInvoicePDF(invoice);

        invoice.setPdfPath(pdfPath);
        invoiceRepository.save(invoice);

        Map<String, Object> response = new HashMap<>();
        response.put("invoice", invoice);
        response.put("pdfPath", pdfPath);
        response.put("message", "Invoice generated successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/{invoiceId}")
    public ResponseEntity<Map<String, String>> sendInvoice(@PathVariable Long invoiceId,
                                                           @RequestParam(defaultValue = "true") boolean markSent) {
        Invoice invoice = invoiceService.getInvoice(invoiceId);

        if (invoice.getPdfPath() == null) {
            String pdfPath = pdfService.generateInvoicePDF(invoice);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);
        }

        emailService.sendInvoiceEmail(invoice, invoice.getPdfPath());

        if (markSent) {
            invoice.setStatus(InvoiceStatus.SENT);
            invoiceRepository.save(invoice);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice sent successfully to " +
                invoice.getPurchaseOrder().getClientCompany().getEmail());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> downloadInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoice(id);
        if (invoice.getPdfPath() == null) {
            String pdfPath = pdfService.generateInvoicePDF(invoice);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);
        }

        File file = new File(invoice.getPdfPath());
        if (!file.exists()) {
            throw new RuntimeException("Invoice PDF not found: " + invoice.getPdfPath());
        }

        String filename = file.getName();
        FileSystemResource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
