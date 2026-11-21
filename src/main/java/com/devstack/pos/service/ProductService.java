package com.devstack.pos.service;

import com.devstack.pos.entity.Product;
import com.devstack.pos.enums.Status;
import com.devstack.pos.repository.ProductRepository;
import com.devstack.pos.util.TerminalConfigManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public Product findProduct(Integer code) {
        return productRepository.findById(code).orElse(null);
    }

    public List<Product> findAllProducts() {
        Long businessId = TerminalConfigManager.getBusinessId();
        if (businessId != null) {
            return productRepository.findByBusinessId(businessId);
        }
        return productRepository.findAll();
    }

    public List<Product> findAllProducts(Long businessId) {
        if (businessId != null) {
            return productRepository.findByBusinessId(businessId);
        }
        return productRepository.findAll();
    }

}
