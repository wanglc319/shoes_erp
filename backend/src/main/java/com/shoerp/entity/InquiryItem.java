package com.shoerp.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Entity
@Table(name = "inquiry_item")
public class InquiryItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry;

    @Column(name = "shoe_id")
    private Long shoeId;

    @Column(name = "style_no", length = 60)
    private String styleNo;

    @Column(name = "product_name", length = 160)
    private String productName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "upper_material", length = 80)
    private String upperMaterial;

    @Column(name = "sole_material", length = 80)
    private String soleMaterial;

    @Column(length = 80)
    private String color;

    @Column(name = "size_range", length = 80)
    private String sizeRange;

    @Column
    private Integer quantity;

    @Column(length = 10)
    private String unit = "PRS";

    @Column(name = "target_price", precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "quoted_price", precision = 12, scale = 2)
    private BigDecimal quotedPrice;

    @Column(length = 300)
    private String remark;
}
