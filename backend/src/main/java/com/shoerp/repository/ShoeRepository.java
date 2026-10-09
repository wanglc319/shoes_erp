package com.shoerp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shoerp.entity.Shoe;

public interface ShoeRepository extends JpaRepository<Shoe, Long>, JpaSpecificationExecutor<Shoe> {

    Optional<Shoe> findByStyleNo(String styleNo);

    boolean existsByStyleNo(String styleNo);

    boolean existsBySupplierId(Long supplierId);

    @Query(value = "SELECT (SELECT COUNT(*) FROM order_item WHERE shoe_id = :shoeId)"
            + " + (SELECT COUNT(*) FROM quotation_item WHERE shoe_id = :shoeId)"
            + " + (SELECT COUNT(*) FROM purchase_order_item WHERE shoe_id = :shoeId)", nativeQuery = true)
    long countItemReferences(@Param("shoeId") Long shoeId);
}
