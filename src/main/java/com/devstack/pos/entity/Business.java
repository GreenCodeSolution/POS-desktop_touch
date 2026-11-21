package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "business")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_code", nullable = false, unique = true, length = 50)
    private String businessCode;

    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "LKR";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, TRIAL, EXPIRED

    @Column(name = "subscription_plan", nullable = false, length = 50)
    @Builder.Default
    private String subscriptionPlan = "TRIAL"; // TRIAL, BASIC, PRO, ENTERPRISE

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "max_terminals", nullable = false)
    @Builder.Default
    private Integer maxTerminals = 5;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currency == null) currency = "LKR";
        if (status == null) status = "ACTIVE";
        if (subscriptionPlan == null) subscriptionPlan = "TRIAL";
        if (maxTerminals == null) maxTerminals = 5;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
