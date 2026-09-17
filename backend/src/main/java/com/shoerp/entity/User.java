package com.shoerp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 120)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(length = 80)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 30)
    private String role = "SALES";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 200)
    private String remark;
}
