package com.shoerp.dto;

import java.util.List;

import lombok.Data;

@Data
public class ShipmentRequest {

    private Long id;

    private String shipmentNo;

    private Long orderId;

    private String shipDate;

    private String containerNo;

    private String blNo;

    private String vessel;

    private String portOfLoading;

    private String portOfDestination;

    private String status;

    private String remark;

    private List<ItemPayload> items;

    @Data
    public static class ItemPayload {
        private String styleNo;
        private String productName;
        private String color;
        private String sizeRange;
        private Integer quantity;
        private String unit;
        private Integer cartons;
        private java.math.BigDecimal grossWeight;
        private java.math.BigDecimal netWeight;
        private java.math.BigDecimal unitPrice;
    }
}
