package com.fitness.userservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true , nullable = false)
    private String email;
    private String keycloakId;
    @Column(nullable = false)
    private String password;
    private String firstName;
    private String lastName;
    // Biometrics from Keycloak (nullable: older users may not have them)
    private Integer age;
    private Double weight;   // kg
    private Double height;   // cm
    private String gender;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedId;



}

