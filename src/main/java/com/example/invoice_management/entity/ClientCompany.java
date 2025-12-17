package com.example.invoice_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "client_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String address;

    @Column(length = 10)
    private String panNumber;

    @Column(length = 15)
    private String gstNumber;

    @Column(nullable = false)
    private String email;

    private String phone;

    private LocalDate createdDate;
}
