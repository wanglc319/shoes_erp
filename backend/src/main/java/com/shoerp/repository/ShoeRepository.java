package com.shoerp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shoerp.entity.Shoe;

public interface ShoeRepository extends JpaRepository<Shoe, Long>, JpaSpecificationExecutor<Shoe> {

    Optional<Shoe> findByStyleNo(String styleNo);

    boolean existsByStyleNo(String styleNo);
}
