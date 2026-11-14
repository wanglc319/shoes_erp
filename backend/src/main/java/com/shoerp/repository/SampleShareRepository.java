package com.shoerp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shoerp.entity.SampleShare;

public interface SampleShareRepository extends JpaRepository<SampleShare, Long> {

    Optional<SampleShare> findByToken(String token);

    List<SampleShare> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<SampleShare> findAllByOrderByCreatedAtDesc();
}
