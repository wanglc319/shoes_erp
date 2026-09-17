package com.shoerp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shoerp.entity.RoleMenu;

public interface RoleMenuRepository extends JpaRepository<RoleMenu, Long> {

    List<RoleMenu> findByRole(String role);

    void deleteByRole(String role);

    boolean existsByRoleAndMenuKey(String role, String menuKey);
}
