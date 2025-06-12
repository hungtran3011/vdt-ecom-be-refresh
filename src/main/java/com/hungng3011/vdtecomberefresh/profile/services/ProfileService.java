package com.hungng3011.vdtecomberefresh.profile.services;

import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.entities.Profile;
import com.hungng3011.vdtecomberefresh.profile.mappers.ProfileMapper;
import com.hungng3011.vdtecomberefresh.profile.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProfileService {

    @Autowired
    private ProfileRepository repository;

    @Autowired
    private ProfileMapper mapper;

    public ProfileDto getProfile(UUID userId) {
        try {
            log.info("Retrieving profile for user id: {}", userId);
            Profile profile = repository.findProfileByUserId(userId)
                    .orElseThrow(() -> new EntityNotFoundException("Profile not found for user ID: " + userId));
            log.info("Successfully retrieved profile for user id: {}", userId);
            return mapper.toDto(profile);
        } catch (EntityNotFoundException e) {
            log.warn("Profile not found for user id: {}", userId);
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving profile for user id: {}", userId, e);
            throw e;
        }
    }

    public ProfileDto getProfileByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must be provided");
        }
        String normalizedEmail = email.trim().toLowerCase();
        try {
            log.info("Retrieving profile for email: {}", normalizedEmail);
            Profile profile = repository.findProfileByEmail(normalizedEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Profile not found for email: " + normalizedEmail));
            log.info("Successfully retrieved profile for email: {}", normalizedEmail);
            return mapper.toDto(profile);
        } catch (EntityNotFoundException e) {
            log.warn("Profile not found for email: {}", normalizedEmail);
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving profile for email: {}", normalizedEmail, e);
            throw e;
        }
    }

    @Transactional
    public ProfileDto createOrUpdate(ProfileDto dto) {
        try {
            log.info("Creating or updating profile for user id: {}", dto.getUserId());
            Profile profile = repository.findProfileByUserId(dto.getUserId())
                    .orElse(new Profile()); // If not found, creates a new Profile instance

            // Map DTO to entity. If profile is new, all fields will be set from DTO.
            // If profile exists, this creates a new entity 'entityToSave' with DTO data.
            Profile entityToSave = mapper.toEntity(dto);

            if (profile.getId() != null) { // If profile was found in DB (i.e., it's an existing profile)
                entityToSave.setId(profile.getId()); // Set the ID to ensure an update, not insert
                log.info("Updating existing profile with id: {} for user: {}", profile.getId(), dto.getUserId());
            } else {
                log.info("Creating new profile for user id: {}", dto.getUserId());
            }
            
            // Ensure userId is set, especially if it's a new profile from DTO
            if (entityToSave.getUserId() == null && dto.getUserId() != null) {
                entityToSave.setUserId(dto.getUserId());
            }

            Profile saved = repository.save(entityToSave);
            log.info("Successfully {} profile for user id: {}", 
                    profile.getId() != null ? "updated" : "created", dto.getUserId());
            return mapper.toDto(saved);
        } catch (Exception e) {
            log.error("Error creating or updating profile for user id: {}", dto.getUserId(), e);
            throw e;
        }
    }

    /**
     * @deprecated Use AuthProfileIntegrationService.syncProfileFromJwt() instead.
     * This method is kept for backward compatibility but will be removed in a future release.
     */
    @Transactional
    @Deprecated
    public ProfileDto syncProfileFromToken(Jwt jwt) {
        String userIdString = jwt.getSubject();
        if (userIdString == null) {
            log.error("JWT 'sub' claim is missing. Cannot sync profile.");
            throw new IllegalArgumentException("JWT 'sub' claim is missing for profile sync.");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdString);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for 'sub' claim: {}. Cannot sync profile.", userIdString, e);
            throw new IllegalArgumentException("Invalid UUID format in JWT 'sub' claim: " + userIdString);
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

        Profile saved = repository.save(profile);
        log.info("Profile synced/updated for userId: {}", userId);
        return mapper.toDto(saved);
    }

    public PagedResponse<ProfileDto> getAllWithPagination(Long cursor, int limit) {
        try {
            log.info("Retrieving profiles with cursor: {} and limit: {}", cursor, limit);
            
            Pageable pageable = PageRequest.of(0, limit);
            List<Profile> profiles;
            
            if (cursor == null) {
                profiles = repository.findAllWithoutCursor(pageable);
            } else {
                profiles = repository.findAllWithCursorAfter(cursor, pageable);
            }
            
            List<ProfileDto> profileDtos = profiles.stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
            
            Long nextCursor = null;
            if (!profiles.isEmpty() && profiles.size() == limit) {
                nextCursor = profiles.get(profiles.size() - 1).getId();
            }
            
            long totalElements = repository.countAllProfiles();
            int totalPages = (int) Math.ceil((double) totalElements / limit);
            
            PagedResponse.PaginationMetadata metadata = PagedResponse.PaginationMetadata.builder()
                    .page(0)
                    .size(limit)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(nextCursor != null)
                    .hasPrevious(cursor != null)
                    .nextCursor(nextCursor)
                    .previousCursor(cursor)
                    .build();
            
            log.info("Successfully retrieved {} profiles", profileDtos.size());
            return PagedResponse.<ProfileDto>builder()
                    .content(profileDtos)
                    .pagination(metadata)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving profiles with pagination", e);
            throw e;
        }
    }

    public PagedResponse<ProfileDto> getAllWithPreviousCursor(Long cursor, int limit) {
        try {
            log.info("Retrieving profiles with previous cursor: {} and limit: {}", cursor, limit);
            
            if (cursor == null) {
                log.warn("Previous cursor is null, returning empty result");
                PagedResponse.PaginationMetadata emptyMetadata = PagedResponse.PaginationMetadata.builder()
                        .page(0)
                        .size(limit)
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .nextCursor(null)
                        .previousCursor(null)
                        .build();
                        
                return PagedResponse.<ProfileDto>builder()
                        .content(Collections.emptyList())
                        .pagination(emptyMetadata)
                        .build();
            }
            
            Pageable pageable = PageRequest.of(0, limit);
            List<Profile> profiles = repository.findAllWithCursorBefore(cursor, pageable);
            
            // Reverse the order since we queried in DESC order
            Collections.reverse(profiles);
            
            List<ProfileDto> profileDtos = profiles.stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
            
            Long nextCursor = cursor;
            Long previousCursor = null;
            if (!profiles.isEmpty()) {
                previousCursor = profiles.get(0).getId();
            }
            
            long totalElements = repository.countAllProfiles();
            int totalPages = (int) Math.ceil((double) totalElements / limit);
            
            PagedResponse.PaginationMetadata metadata = PagedResponse.PaginationMetadata.builder()
                    .page(Math.max(0, 0 - 1))
                    .size(limit)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(true)
                    .hasPrevious(!profiles.isEmpty() && profiles.size() == limit)
                    .nextCursor(nextCursor)
                    .previousCursor(previousCursor)
                    .build();
            
            log.info("Successfully retrieved {} profiles with previous cursor", profileDtos.size());
            return PagedResponse.<ProfileDto>builder()
                    .content(profileDtos)
                    .pagination(metadata)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving profiles with previous cursor", e);
            throw e;
        }
    }
}