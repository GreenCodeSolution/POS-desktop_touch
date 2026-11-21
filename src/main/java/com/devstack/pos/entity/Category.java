package com.devstack.pos.entity;

import com.devstack.pos.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category", uniqueConstraints = {
    @UniqueConstraint(name = "uk_business_category_name", columnNames = {"business_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    public Category(String name) {
        this.name = name;
        this.status = Status.ACTIVE;
    }
}
