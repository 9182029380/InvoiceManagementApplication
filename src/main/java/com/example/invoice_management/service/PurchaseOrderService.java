package com.example.invoice_management.service;

import com.example.invoice_management.entity.POStatus;
import com.example.invoice_management.entity.PurchaseOrder;
import com.example.invoice_management.entity.ClientCompany;
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
        // Ensure client company exists and populate PAN/GST from client record
        if (po.getClientCompany() == null || po.getClientCompany().getId() == null) {
            throw new RuntimeException("Client company is required for creating a PO");
        }

        final Long clientId = po.getClientCompany().getId();

        ClientCompany client = clientCompanyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));

        // Pull PAN and GST from client record
        po.setClientPanNumber(client.getPanNumber());
        po.setClientGstNumber(client.getGstNumber());

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
