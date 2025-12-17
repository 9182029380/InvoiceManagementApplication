package com.example.invoice_management.repository;

import com.example.invoice_management.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
}
