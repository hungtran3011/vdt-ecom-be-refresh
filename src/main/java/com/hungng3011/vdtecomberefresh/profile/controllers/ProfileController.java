package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
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

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PostMapping
    public ResponseEntity<ProfileDto> saveProfile(@RequestBody ProfileDto dto) {
        return ResponseEntity.ok(profileService.createOrUpdate(dto));
    }

    @PostMapping("/me/sync")
    public ResponseEntity<Void> syncMyProfile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            // This case should ideally be handled by Spring Security if endpoint is secured
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        profileService.syncProfileFromToken(jwt);
        return ResponseEntity.ok().build();
    }
}