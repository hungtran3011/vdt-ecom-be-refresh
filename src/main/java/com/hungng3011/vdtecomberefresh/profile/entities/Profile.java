package com.hungng3011.vdtecomberefresh.profile.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

import static jakarta.persistence.GenerationType.*;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID userId; // Matches Keycloak user ID

    private String fullName;
    private String phone;
    private String email;
    private  LocalDate dateOfBirth;

    @Embedded
    private Address address;
}

