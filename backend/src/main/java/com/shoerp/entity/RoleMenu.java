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
@Table(name = "sys_role_menu", uniqueConstraints = @UniqueConstraint(columnNames = {"role", "menu_key"}))
public class RoleMenu extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "menu_key", nullable = false, length = 50)
    private String menuKey;

    public RoleMenu() {
    }

    public RoleMenu(String role, String menuKey) {
        this.role = role;
        this.menuKey = menuKey;
    }
}
