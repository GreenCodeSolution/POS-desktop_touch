package com.devstack.pos.repository;

import com.devstack.pos.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findByBusinessCode(String businessCode);
    boolean existsByBusinessCode(String businessCode);
    List<Business> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
