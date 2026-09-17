package com.shoerp.dto;

import java.util.List;

import lombok.Data;

@Data
public class QuotationRequest {

    private Long id;

    private String quoteNo;

    private Long customerId;

    private String quoteDate;

    private String validUntil;

    private String tradeTerms;

    private String currency;

    private String paymentTerms;

    private String status;

    private String salesOwner;

    private String remark;

    private List<ItemPayload> items;

    @Data
    public static class ItemPayload {
        private Long shoeId;
        private String styleNo;
        private String productName;
        private String material;
        private String color;
        private String sizeRange;
        private Integer quantity;
        private String unit;
        private java.math.BigDecimal unitPrice;
        private java.math.BigDecimal costPrice;
        private String remark;
    }
}
