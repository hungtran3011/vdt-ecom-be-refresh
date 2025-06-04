package com.hungng3011.vdtecomberefresh.profile.services;

import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.entities.Profile;
import com.hungng3011.vdtecomberefresh.profile.mappers.ProfileMapper;
import com.hungng3011.vdtecomberefresh.profile.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private ProfileRepository repository;

    @Autowired
    private ProfileMapper mapper;

    public ProfileDto getProfile(UUID userId) {
        Profile profile = repository.findProfileByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user ID: " + userId));
        return mapper.toDto(profile);
    }

    @Transactional
    public ProfileDto createOrUpdate(ProfileDto dto) {
        Profile profile = repository.findProfileByUserId(dto.getUserId())
                .orElse(new Profile()); // If not found, creates a new Profile instance

        // Map DTO to entity. If profile is new, all fields will be set from DTO.
        // If profile exists, this creates a new entity 'entityToSave' with DTO data.
        Profile entityToSave = mapper.toEntity(dto);

        if (profile.getId() != null) { // If profile was found in DB (i.e., it's an existing profile)
            entityToSave.setId(profile.getId()); // Set the ID to ensure an update, not insert
        }
        // If profile was not found, profile.getId() is null.
        // entityToSave.getId() is also null (assuming DTO doesn't map an ID or it's null).
        // So, it will be an insert. userId must be set from DTO.
        // Ensure userId is set, especially if it's a new profile from DTO
        if (entityToSave.getUserId() == null && dto.getUserId() != null) {
            entityToSave.setUserId(dto.getUserId());
        }


        Profile saved = repository.save(entityToSave);
        return mapper.toDto(saved);
    }

    @Transactional
    public void syncProfileFromToken(Jwt jwt) {
        String userIdString = jwt.getSubject();
        if (userIdString == null) {
            log.error("JWT 'sub' claim is missing. Cannot sync profile.");
            // Depending on requirements, you might throw an exception here
            // throw new IllegalArgumentException("JWT 'sub' claim is missing for profile sync.");
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdString);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for 'sub' claim: {}. Cannot sync profile.", userIdString, e);
            // throw new IllegalArgumentException("Invalid UUID format in JWT 'sub' claim.");
            return;
        }

        String email = jwt.getClaimAsString("email");
        String fullName = jwt.getClaimAsString("name"); // Standard OIDC claim for full name


        Profile profile = repository.findProfileByUserId(userId)
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    newProfile.setUserId(userId); // Set the mandatory userId for new profiles
                    log.info("Creating new profile for userId: {}", userId);
                    return newProfile;
                });

        // Update relevant fields from JWT if they are provided
        // This ensures that existing, user-managed fields (like phone, address) are not overwritten by nulls from JWT
        if (email != null) {
            profile.setEmail(email);
        }
        if (fullName != null) {
            profile.setFullName(fullName);
        }

        repository.save(profile);
        log.info("Profile synced/updated for userId: {}", userId);
    }
}