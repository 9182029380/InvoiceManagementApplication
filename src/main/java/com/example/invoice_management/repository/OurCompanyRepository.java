package com.example.invoice_management.repository;

import com.example.invoice_management.entity.ClientCompany;
import com.example.invoice_management.entity.Invoice;
import com.example.invoice_management.entity.OurCompany;
import com.example.invoice_management.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OurCompanyRepository extends JpaRepository<OurCompany, Long> {
    Optional<OurCompany> findByCompanyId(String companyId);
    boolean existsByCompanyId(String companyId);
}

