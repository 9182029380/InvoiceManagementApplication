package com.example.invoice_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "purchase_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    @Id
    @Column(unique = true, nullable = false)
    private String poNumber; // PO Number is the ID

    @ManyToOne
    @JoinColumn(name = "client_company_id", nullable = false)
    private ClientCompany clientCompany;

    @Column(nullable = false)
    private String trainingDetails;

    @Column(nullable = false)
    private Double trainingAmount;

    @Column(nullable = false)
    private Double gstPercentage; // 18%

    @Column(nullable = false)
    private Double gstAmount;

    @Column(nullable = false)
    private Double totalAmount;

    private LocalDate poDate;

    @Column(nullable = false)
    private String clientPanNumber;

    @Column(nullable = false)
    private String clientGstNumber;

    @Enumerated(EnumType.STRING)
    private POStatus status;

    private LocalDate createdDate;
}
