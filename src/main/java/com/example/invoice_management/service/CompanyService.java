package com.example.invoice_management.service;

import com.example.invoice_management.entity.*;
import com.example.invoice_management.repository.ClientCompanyRepository;
import com.example.invoice_management.repository.InvoiceRepository;
import com.example.invoice_management.repository.OurCompanyRepository;
import com.example.invoice_management.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final OurCompanyRepository ourCompanyRepository;
    private final ClientCompanyRepository clientCompanyRepository;
    private final Random random = new Random();

    public String generateCompanyId() {
        String companyId;
        do {
            companyId = String.format("%06d", random.nextInt(1000000));
        } while (ourCompanyRepository.existsByCompanyId(companyId));
        return companyId;
    }

    @Transactional
    public OurCompany createOurCompany(OurCompany company) {
        company.setCompanyId(generateCompanyId());
        company.setCreatedDate(LocalDate.now());
        return ourCompanyRepository.save(company);
    }

    public OurCompany getOurCompany() {
        return ourCompanyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Our company not configured"));
    }

    @Transactional
    public ClientCompany createClientCompany(ClientCompany company) {
        company.setCreatedDate(LocalDate.now());
        return clientCompanyRepository.save(company);
    }

    public List<ClientCompany> getAllClientCompanies() {
        return clientCompanyRepository.findAll();
    }
}

