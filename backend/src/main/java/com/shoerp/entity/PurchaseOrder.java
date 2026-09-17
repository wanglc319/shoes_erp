package com.shoerp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "purchase_order", uniqueConstraints = @UniqueConstraint(columnNames = "purchase_no"))
public class PurchaseOrder extends BaseEntity {

    @Column(name = "purchase_no", nullable = false, length = 40)
    private String purchaseNo;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_name", length = 120)
    private String supplierName;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(length = 60)
    private String currency = "CNY";

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "buyer", length = 60)
    private String buyer;

    @Column(length = 500)
    private String remark;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
