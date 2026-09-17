package com.shoerp.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shipment_item")
public class ShipmentItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "style_no", length = 60)
    private String styleNo;

    @Column(name = "product_name", length = 160)
    private String productName;

    @Column(length = 120)
    private String color;

    @Column(name = "size_range", length = 80)
    private String sizeRange;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false, length = 20)
    private String unit = "PRS";

    @Column(nullable = false)
    private Integer cartons = 0;

    @Column(name = "gross_weight", precision = 12, scale = 2)
    private BigDecimal grossWeight;

    @Column(name = "net_weight", precision = 12, scale = 2)
    private BigDecimal netWeight;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}
