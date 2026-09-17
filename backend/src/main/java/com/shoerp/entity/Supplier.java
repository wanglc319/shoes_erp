package com.shoerp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "supplier", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Supplier extends BaseEntity {

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

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(length = 30)
    private String type;

    @Column(nullable = false)
    private Integer rating = 5;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 300)
    private String remark;
}
