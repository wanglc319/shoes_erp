package com.shoerp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shoerp.entity.Quotation;

public interface QuotationRepository extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {

    Optional<Quotation> findByQuoteNo(String quoteNo);

    boolean existsByQuoteNo(String quoteNo);

    long countByStatus(String status);
}
