package com.shoerp.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseRequest {

    private Long id;

    private String purchaseNo;

    private Long supplierId;

    private String purchaseDate;

    private String expectedDate;

    private String currency = "CNY";

    private String paymentTerms;

    private String status;

    private String buyer;

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

        private BigDecimal unitPrice;

        private String remark;
    }
}
