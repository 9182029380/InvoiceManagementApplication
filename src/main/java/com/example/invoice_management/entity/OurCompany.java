package com.example.invoice_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "our_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OurCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 6)
    private String companyId; // 6 digit random

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String address;

    @Column(unique = true, nullable = false, length = 10)
    private String panNumber;

    @Column(unique = true, nullable = false, length = 15)
    private String gstNumber;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String ifscCode;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private LocalDate createdDate;
}

