package com.shoerp.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_en", length = 120)
    private String nameEn;

    @Column(name = "contact_person", length = 60)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 80)
    private String email;

    @Column(length = 200)
    private String address;

    @Column(length = 60)
    private String country;

    @Column(length = 30)
    private String level = "B";

    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(name = "tax_no", length = 60)
    private String taxNo;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 300)
    private String remark;
}
