package com.devstack.pos.service;

import com.devstack.pos.entity.Product;
import com.devstack.pos.enums.Status;
import com.devstack.pos.repository.ProductRepository;
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
        return productRepository.findAll();
    }

}
