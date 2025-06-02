package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
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
}

