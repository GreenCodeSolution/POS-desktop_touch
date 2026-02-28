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

    public Product saveProduct(Product product) {
        System.out.println("[PRODUCT SERVICE DEBUG] ========== saveProduct() called ==========");
        System.out.println("[PRODUCT SERVICE DEBUG] Product description: '" + product.getDescription() + "'");
        System.out.println("[PRODUCT SERVICE DEBUG] Product code: " + product.getCode());

        if (product.getStatus() == null) {
            product.setStatus(Status.ACTIVE);
        }

        System.out.println("[PRODUCT SERVICE DEBUG] About to save product to repository...");
        Product savedProduct = productRepository.save(product);
        System.out.println("[PRODUCT SERVICE DEBUG] Product saved to repository!");
        System.out.println("[PRODUCT SERVICE DEBUG] Saved product code: " + savedProduct.getCode());

        return savedProduct;
    }

    public boolean updateProduct(Product product) {
        if (productRepository.existsById(Math.toIntExact(product.getCode()))) {
            productRepository.save(product);
            return true;
        }
        return false;
    }

    public boolean deleteProduct(Integer code) {
        if (productRepository.existsById(code)) {
            productRepository.deleteById(code);
            return true;
        }
        return false;
    }

    public Product findProduct(Integer code) {
        return productRepository.findById(code).orElse(null);
    }

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public int getLastProductId() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return 0;
        }
        return products.stream()
                .mapToInt(Product::getCode)
                .max()
                .orElse(0);
    }
}
