package com.devstack.pos.repository;

import com.devstack.pos.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByBusinessIdOrderByIdAsc(Long businessId);

    @Query("SELECT c FROM Customer c WHERE (c.businessId IS NULL OR c.businessId = :businessId) AND (c.name LIKE %:search% OR c.contact LIKE %:search%) ORDER BY c.id ASC")
    List<Customer> searchCustomersByBusiness(@Param("businessId") Long businessId, @Param("search") String search);

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:search% OR c.contact LIKE %:search% ORDER BY c.id ASC")
    List<Customer> searchCustomers(@Param("search") String search);
    
    @Query("SELECT c FROM Customer c ORDER BY c.id ASC")
    List<Customer> findAll();
}

