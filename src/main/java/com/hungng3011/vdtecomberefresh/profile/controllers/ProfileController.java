package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.hungng3011.vdtecomberefresh.auth.services.AuthProfileService;
import com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/profiles") 
@Slf4j
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private AuthProfileService authProfileService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProfileDto>> getAllProfiles(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean previous) {
        
        log.info("Fetching profiles with cursor: {}, limit: {}, previous: {}", cursor, limit, previous);
        try {
            PagedResponse<ProfileDto> response;
            if (previous) {
                response = profileService.getAllWithPreviousCursor(cursor, limit);
            } else {
                response = profileService.getAllWithPagination(cursor, limit);
            }
            log.info("Successfully retrieved {} profiles", response.getContent().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching profiles with cursor: {}, limit: {}, previous: {}", cursor, limit, previous, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID userId) {
        log.info("Fetching profile for user ID: {}", userId);
        try {
            ProfileDto profile = profileService.getProfile(userId);
            log.info("Successfully retrieved profile for user ID: {}", userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching profile for user ID: {}", userId, e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<ProfileDto> saveProfile(@RequestBody ProfileDto dto) {
        log.info("Saving profile for user ID: {}", dto.getUserId());
        try {
            // 1. First, save the profile to our database
            ProfileDto savedProfile = profileService.createOrUpdate(dto);
            
            // 2. Then, synchronize the changes back to Keycloak
            // This ensures Keycloak has the latest data
            authProfileService.updateKeycloakFromProfile(savedProfile);
            
            log.info("Successfully saved and synced profile for user ID: {}", dto.getUserId());
            return ResponseEntity.ok(savedProfile);
        } catch (EntityNotFoundException e) {
            log.warn("Profile not found for user ID: {}", dto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid profile data for user ID: {}", dto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error saving profile for user ID: {}", dto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/me/sync")
    public ResponseEntity<ProfileDto> syncMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Syncing profile from JWT for user ID: {}", userId);
        try {
            ProfileDto profileDto = authProfileService.syncProfileFromJwt(jwt);
            log.info("Successfully synced profile from JWT for user ID: {}", userId);
            return ResponseEntity.ok(profileDto);
        } catch (EntityNotFoundException e) {
            // This is the specific case when a Keycloak user exists but doesn't have a corresponding profile
            log.warn("Profile not found during JWT sync for user ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        } catch (IllegalArgumentException e) {
            // Invalid JWT token data
            log.warn("Invalid JWT token data for user ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .build();
        } catch (Exception e) {
            // General error handling
            log.error("Error syncing profile from JWT for user ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}