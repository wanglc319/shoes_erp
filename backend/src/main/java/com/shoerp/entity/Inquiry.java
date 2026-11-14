package com.shoerp.entity;

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

/**
 * 询价单：业务员选好鞋款后发给工厂询价。
 */
@Getter
@Setter
@Entity
@Table(name = "inquiry", uniqueConstraints = @UniqueConstraint(columnNames = "inquiry_no"))
public class Inquiry extends OwnedEntity {

    @Column(name = "inquiry_no", nullable = false, length = 40)
    private String inquiryNo;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", length = 120)
    private String supplierName;

    @Column(name = "inquiry_date", nullable = false)
    private LocalDate inquiryDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(length = 10)
    private String currency = "CNY";

    @Column(length = 500)
    private String remark;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<InquiryItem> items = new ArrayList<>();
}
