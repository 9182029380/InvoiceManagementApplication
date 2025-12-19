package com.example.invoice_management.controller;

import com.example.invoice_management.entity.PurchaseOrder;
import com.example.invoice_management.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/po")
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderService poService;

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPO(@RequestBody PurchaseOrder po) {
        return ResponseEntity.ok(poService.createPurchaseOrder(po));
    }

    @GetMapping("/{poNumber}")
    public ResponseEntity<PurchaseOrder> getPO(@PathVariable String poNumber) {
        return ResponseEntity.ok(poService.getPurchaseOrder(poNumber));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPOs() {
        return ResponseEntity.ok(poService.getAllPurchaseOrders());
    }

    @PutMapping("/{poNumber}")
    public ResponseEntity<PurchaseOrder> updatePO(@PathVariable String poNumber,
                                                  @RequestBody PurchaseOrder po) {
        return ResponseEntity.ok(poService.updatePurchaseOrder(poNumber, po));
    }

    @DeleteMapping("/{poNumber}")
    public ResponseEntity<Void> deletePO(@PathVariable String poNumber,
                                         @RequestParam(defaultValue = "false") boolean force) {
        poService.deletePurchaseOrder(poNumber, force);
        return ResponseEntity.noContent().build();
    }
}
