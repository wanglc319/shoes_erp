package com.shoerp.dto;

import java.util.List;

import lombok.Data;

@Data
public class OrderRequest {

    private Long id;

    private String orderNo;

    private Long quotationId;

    private Long customerId;

    private String orderDate;

    private String deliveryDate;

    private String tradeTerms;

    private String currency;

    private String paymentTerms;

    private java.math.BigDecimal depositAmount;

    private String status;

    private String productionStatus;

    private String salesOwner;

    private String remark;

    private List<ItemPayload> items;

    @Data
    public static class ItemPayload {
        private Long shoeId;
        private String styleNo;
        private String productName;
        private String color;
        private String sizeRange;
        private Integer quantity;
        private String unit;
        private java.math.BigDecimal unitPrice;
        private java.math.BigDecimal costPrice;
        private String remark;
    }
}
