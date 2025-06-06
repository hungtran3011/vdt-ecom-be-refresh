package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.hungng3011.vdtecomberefresh.auth.services.AuthProfileService;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/profiles") // Ensure this matches your desired base path
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private AuthProfileService authProfileService;

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PostMapping
    public ResponseEntity<ProfileDto> saveProfile(@RequestBody ProfileDto dto) {
        try {
            // 1. First, save the profile to our database
            ProfileDto savedProfile = profileService.createOrUpdate(dto);
            
            // 2. Then, synchronize the changes back to Keycloak
            // This ensures Keycloak has the latest data
            authProfileService.updateKeycloakFromProfile(savedProfile);
            
            return ResponseEntity.ok(savedProfile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/me/sync")
    public ResponseEntity<ProfileDto> syncMyProfile(@AuthenticationPrincipal Jwt jwt) {
        try {
            ProfileDto profileDto = authProfileService.syncProfileFromJwt(jwt);
            return ResponseEntity.ok(profileDto);
        } catch (EntityNotFoundException e) {
            // This is the specific case when a Keycloak user exists but doesn't have a corresponding profile
            // We could handle it differently than general errors if needed
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        } catch (IllegalArgumentException e) {
            // Invalid JWT token data
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .build();
        } catch (Exception e) {
            // General error handling
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}