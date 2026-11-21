package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_terminal", uniqueConstraints = {
    @UniqueConstraint(name = "uk_business_terminal_code", columnNames = {"business_id", "terminal_code"}),
    @UniqueConstraint(name = "uk_pairing_key", columnNames = {"pairing_key"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTerminal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "terminal_code", nullable = false, length = 50)
    private String terminalCode; // e.g. "TERM-01"

    @Column(name = "terminal_name", nullable = false, length = 100)
    private String terminalName; // e.g. "Main Cashier Counter 1"

    @Column(name = "pairing_key", nullable = false, length = 50)
    private String pairingKey; // e.g. "111-222"

    @Column(name = "device_uuid", length = 200)
    private String deviceUuid; // Hardware or installed UUID when paired

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, REVOKED

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
