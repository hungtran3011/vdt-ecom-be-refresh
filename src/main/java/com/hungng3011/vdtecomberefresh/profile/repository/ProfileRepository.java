package com.hungng3011.vdtecomberefresh.profile.repository;

import com.hungng3011.vdtecomberefresh.profile.entities.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    // Custom query methods can be defined here if needed
    // For example, to find a profile by user ID:
     Optional<Profile> findProfileByUserId(UUID userId);

    // Pagination methods
    @Query("SELECT p FROM Profile p WHERE p.id > :cursorId ORDER BY p.id ASC")
    List<Profile> findAllWithCursorAfter(@Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT p FROM Profile p WHERE p.id < :cursorId ORDER BY p.id DESC")
    List<Profile> findAllWithCursorBefore(@Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT p FROM Profile p ORDER BY p.id ASC")
    List<Profile> findAllWithoutCursor(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Profile p")
    long countAllProfiles();
}
