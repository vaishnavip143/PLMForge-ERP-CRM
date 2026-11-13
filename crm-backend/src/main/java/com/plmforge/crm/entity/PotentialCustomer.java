package com.plmforge.crm.entity;

import com.plmforge.crm.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "potential_customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PotentialCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "phone_number", length = 50)
    private String phone;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW_LEAD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = LeadStatus.NEW_LEAD;
        }
        if (source == null) {
            source = "INCOMING_QUERY";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
