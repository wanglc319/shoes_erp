package com.shoerp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shoerp.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    boolean existsByPaymentNo(String paymentNo);

    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

    List<Payment> findByOrderIdIn(List<Long> orderIds);
}
