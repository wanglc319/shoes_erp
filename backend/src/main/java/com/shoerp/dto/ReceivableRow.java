package com.shoerp.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ReceivableRow {

    private Long orderId;

    private String orderNo;

    private String customerName;

    private String orderDate;

    private String currency;

    private String paymentTerms;

    private BigDecimal totalAmount;

    private BigDecimal receivedAmount;

    private BigDecimal outstandingAmount;

    private String status;

    private boolean overdue;

    private List<String> paymentNos;
}
