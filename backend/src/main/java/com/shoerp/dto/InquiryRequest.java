package com.shoerp.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class InquiryRequest {

    private Long id;

    private String inquiryNo;

    private Long supplierId;

    private String inquiryDate;

    private String deadline;

    private String status;

    private String currency;

    private String remark;

    private List<ItemPayload> items;

    @Data
    public static class ItemPayload {

        private Long shoeId;

        private String styleNo;

        private String productName;

        private String imageUrl;

        private String upperMaterial;

        private String soleMaterial;

        private String color;

        private String sizeRange;

        private Integer quantity;

        private String unit;

        private BigDecimal targetPrice;

        private BigDecimal quotedPrice;

        private String remark;
    }
}
