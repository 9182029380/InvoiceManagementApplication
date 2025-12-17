package com.example.invoice_management.service;

import com.example.invoice_management.entity.*;
import com.example.invoice_management.repository.InvoiceRepository;
import com.example.invoice_management.repository.OurCompanyRepository;
import com.example.invoice_management.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository poRepository;
    private final OurCompanyRepository ourCompanyRepository;
    private final PurchaseOrderService poService;
    private final CompanyService companyService;
    private final Random random = new Random();

    public String generateInvoiceNumber() {
        String invoiceNumber;
        do {
            int digits = random.nextInt(1000);
            String letters = generateRandomLetters(3);
            invoiceNumber = String.format("%03d%s", digits, letters);
        } while (invoiceRepository.existsByInvoiceNumber(invoiceNumber));
        return invoiceNumber;
    }

    private String generateRandomLetters(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        return sb.toString();
    }

    @Transactional
    public Invoice generateInvoice(String companyId, String poNumber) {
        OurCompany ourCompany = companyService.getOurCompany();
        PurchaseOrder po = poService.getPurchaseOrder(poNumber);

        if (po.getStatus() == POStatus.INVOICED) {
            throw new RuntimeException("Invoice already generated for this PO");
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .ourCompany(ourCompany)
                .purchaseOrder(po)
                .invoiceDate(LocalDate.now())
                .subtotal(po.getTrainingAmount())
                .gstAmount(po.getGstAmount())
                .totalAmount(po.getTotalAmount())
                .status(InvoiceStatus.GENERATED)
                .createdDate(LocalDate.now())
                .build();

        invoice = invoiceRepository.save(invoice);
        poService.updatePOStatus(poNumber, POStatus.INVOICED);

        return invoice;
    }

    public Invoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }
}
