package com.shoerp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shoerp.entity.SalesOrder;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    Optional<SalesOrder> findByOrderNo(String orderNo);

    boolean existsByOrderNo(String orderNo);

    boolean existsByCustomerId(Long customerId);

    boolean existsByQuotationId(Long quotationId);

    long countByStatus(String status);
}
