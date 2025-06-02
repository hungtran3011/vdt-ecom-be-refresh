package com.hungng3011.vdtecomberefresh.profile.services;

import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.entities.Profile;
import com.hungng3011.vdtecomberefresh.profile.mappers.ProfileMapper;
import com.hungng3011.vdtecomberefresh.profile.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository repository;

    @Autowired
    private ProfileMapper mapper;

    public ProfileDto getProfile(UUID userId) {
        Profile profile = repository.findProfileByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
        return mapper.toDto(profile);
    }

    public ProfileDto createOrUpdate(ProfileDto dto) {
        Profile profile = repository.findProfileByUserId(dto.getUserId())
                .orElse(new Profile());
        Profile updated = mapper.toEntity(dto);
        if (profile.getId() != null) {
            updated.setId(profile.getId());
        }
        Profile saved = repository.save(updated);
        return mapper.toDto(saved);
    }

//    public void syncProfileFromToken(Jwt jwt) {
//        String sub = jwt.getSubject();
//        String email = jwt.getClaimAsString("email");
//        String fullName = jwt.getClaimAsString("name");
//        repository.findProfileByUserId(sub)
//    }
}
