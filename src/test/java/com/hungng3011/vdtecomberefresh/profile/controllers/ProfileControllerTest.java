package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@ActiveProfiles("test")
class ProfileControllerTest {

    @TestConfiguration
    static class ProfileControllerTestConfiguration {
        @Bean
        public ProfileService profileService() {
            return Mockito.mock(ProfileService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileService profileService; // Autowire the mock bean

    @Autowired
    private ObjectMapper objectMapper;

    private ProfileDto profileDto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileDto = new ProfileDto();
        profileDto.setUserId(userId);
        profileDto.setFullName("Test User");
        profileDto.setEmail("test@example.com");
        profileDto.setPhone("1234567890");
        profileDto.setDateOfBirth(LocalDate.of(1990, 1, 1));
    }

    @Test
    void getProfile_ShouldReturnProfileDto_WhenProfileExists() throws Exception {
        when(profileService.getProfile(userId)).thenReturn(profileDto);

        mockMvc.perform(get("/v1/profiles/{userId}", userId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void saveProfile_ShouldReturnProfileDto_WhenProfileIsValid() throws Exception {
        when(profileService.createOrUpdate(any(ProfileDto.class))).thenReturn(profileDto);

        mockMvc.perform(post("/v1/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileDto))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void syncMyProfile_ShouldReturnOk_WhenJwtIsValid() throws Exception {
        Jwt mockJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .claim("email", "jwtuser@example.com")
                .claim("name", "JWT User")
                .build();

        doNothing().when(profileService).syncProfileFromToken(any(Jwt.class));

        mockMvc.perform(post("/v1/profiles/me/sync")
                        .with(jwt().jwt(mockJwt)))
                .andExpect(status().isOk());
    }

    @Test
    void syncMyProfile_ShouldReturnUnauthorized_WhenNoJwt() throws Exception {
        mockMvc.perform(post("/v1/profiles/me/sync"))
                .andExpect(status().isUnauthorized());
    }
}