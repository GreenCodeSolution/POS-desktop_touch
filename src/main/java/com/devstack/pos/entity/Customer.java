package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "contact", length = 50)
    private String contact;

    @Column(name = "loyalty_points")
    private double loyaltyPoints = 0.0;

    // Constructor without loyalty points for legacy support
    public Customer(Long id, String name, String contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
    }

    // Constructor without ID for creating new customers
    public Customer(String name, String contact) {
        this.name = name;
        this.contact = contact;
        this.loyaltyPoints = 0.0;
    }
}
