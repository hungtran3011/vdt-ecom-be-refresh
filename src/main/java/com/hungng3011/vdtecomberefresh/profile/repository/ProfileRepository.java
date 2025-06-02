package com.hungng3011.vdtecomberefresh.profile.repository;

import com.hungng3011.vdtecomberefresh.profile.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    // Custom query methods can be defined here if needed
    // For example, to find a profile by user ID:
     Optional<Profile> findProfileByUserId(UUID userId);
}
