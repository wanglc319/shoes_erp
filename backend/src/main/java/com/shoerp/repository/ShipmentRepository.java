package com.shoerp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shoerp.entity.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByShipmentNo(String shipmentNo);

    boolean existsByShipmentNo(String shipmentNo);

    List<Shipment> findByOrderId(Long orderId);
}
