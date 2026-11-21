package com.devstack.pos.repository;

import com.devstack.pos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByBusinessId(Long businessId);
    Optional<Product> findByBusinessIdAndBarcode(Long businessId, String barcode);
    Optional<Product> findByBarcode(String barcode);
}
