package com.example.invoice_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 6)
    private String invoiceNumber; // 3 digits + 3 capital letters

    @ManyToOne
    @JoinColumn(name = "our_company_id", nullable = false)
    private OurCompany ourCompany;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private Double subtotal;

    @Column(nullable = false)
    private Double gstAmount;

    @Column(nullable = false)
    private Double totalAmount;

    private String pdfPath;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private LocalDate createdDate;
}
