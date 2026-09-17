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
@Table(name = "order_item")
public class OrderItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder order;

    @Column(name = "shoe_id")
    private Long shoeId;

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

    @Column(name = "shipped_quantity", nullable = false)
    private Integer shippedQuantity = 0;

    @Column(nullable = false, length = 20)
    private String unit = "PRS";

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(length = 300)
    private String remark;
}
