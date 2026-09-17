package com.shoerp.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shoe", uniqueConstraints = @UniqueConstraint(columnNames = "style_no"))
public class Shoe extends BaseEntity {

    @Column(name = "style_no", nullable = false, length = 60)
    private String styleNo;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_en", length = 120)
    private String nameEn;

    @Column(length = 60)
    private String category;

    @Column(length = 40)
    private String gender;

    @Column(name = "upper_material", length = 80)
    private String upperMaterial;

    @Column(name = "sole_material", length = 80)
    private String soleMaterial;

    @Column(length = 120)
    private String color;

    @Column(name = "size_range", length = 80)
    private String sizeRange;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "moq")
    private Integer moq;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "shoe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ShoeImage> images = new ArrayList<>();

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Boolean enabled = true;
}
