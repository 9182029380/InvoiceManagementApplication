package com.example.invoice_management.controller;


import com.example.invoice_management.entity.*;
import com.example.invoice_management.repository.InvoiceRepository;
import com.example.invoice_management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping("/our")
    public ResponseEntity<OurCompany> createOurCompany(@RequestBody OurCompany company) {
        return ResponseEntity.ok(companyService.createOurCompany(company));
    }

    @GetMapping("/our")
    public ResponseEntity<OurCompany> getOurCompany() {
        return ResponseEntity.ok(companyService.getOurCompany());
    }

    @PostMapping("/client")
    public ResponseEntity<ClientCompany> createClientCompany(@RequestBody ClientCompany company) {
        return ResponseEntity.ok(companyService.createClientCompany(company));
    }

    @GetMapping("/client")
    public ResponseEntity<List<ClientCompany>> getAllClientCompanies() {
        return ResponseEntity.ok(companyService.getAllClientCompanies());
    }
}

