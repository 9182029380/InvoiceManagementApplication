package com.example.invoice_management.service;

import com.example.invoice_management.entity.POStatus;
import com.example.invoice_management.entity.PurchaseOrder;
import com.example.invoice_management.repository.ClientCompanyRepository;
import com.example.invoice_management.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final PurchaseOrderRepository poRepository;
    private final ClientCompanyRepository clientCompanyRepository;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrder po) {
        // Calculate GST (18%)
        po.setGstPercentage(18.0);
        po.setGstAmount(po.getTrainingAmount() * 0.18);
        po.setTotalAmount(po.getTrainingAmount() + po.getGstAmount());
        po.setStatus(POStatus.PENDING);
        po.setCreatedDate(LocalDate.now());
        po.setPoDate(LocalDate.now());

        return poRepository.save(po);
    }

    public PurchaseOrder getPurchaseOrder(String poNumber) {
        return poRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new RuntimeException("PO not found: " + poNumber));
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return poRepository.findAll();
    }

    @Transactional
    public void updatePOStatus(String poNumber, POStatus status) {
        PurchaseOrder po = getPurchaseOrder(poNumber);
        po.setStatus(status);
        poRepository.save(po);
    }
}
