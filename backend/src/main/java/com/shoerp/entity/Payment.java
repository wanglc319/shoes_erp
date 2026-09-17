package com.shoerp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment", uniqueConstraints = @UniqueConstraint(columnNames = "payment_no"))
public class Payment extends BaseEntity {

    @Column(name = "payment_no", nullable = false, length = 40)
    private String paymentNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", length = 40)
    private String orderNo;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 30)
    private String method = "T/T";

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 60)
    private String reference;

    @Column(length = 300)
    private String remark;
}
