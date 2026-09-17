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
@Table(name = "sales_order", uniqueConstraints = @UniqueConstraint(columnNames = "order_no"))
public class SalesOrder extends BaseEntity {

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "quotation_id")
    private Long quotationId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "trade_terms", length = 40)
    private String tradeTerms;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "production_status", length = 30)
    private String productionStatus = "NOT_STARTED";

    @Column(name = "sales_owner", length = 50)
    private String salesOwner;

    @Column(length = 500)
    private String remark;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();
}
