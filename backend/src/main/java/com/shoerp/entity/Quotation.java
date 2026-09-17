package com.shoerp.entity;

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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "quotation", uniqueConstraints = @UniqueConstraint(columnNames = "quote_no"))
public class Quotation extends BaseEntity {

    @Column(name = "quote_no", nullable = false, length = 40)
    private String quoteNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "trade_terms", length = 40)
    private String tradeTerms;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "sales_owner", length = 50)
    private String salesOwner;

    @Column(length = 500)
    private String remark;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<QuotationItem> items = new ArrayList<>();
}
