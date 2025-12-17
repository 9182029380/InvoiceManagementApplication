package com.example.invoice_management.dto;

import lombok.Data;

@Data
public class PORequest {
    private String poNumber;
    private Long clientCompanyId;
    private String trainingDetails;
    private Double trainingAmount;
    private String clientPanNumber;
    private String clientGstNumber;
}
