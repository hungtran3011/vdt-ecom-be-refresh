package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.auth.services.AuthProfileService;
import com.hungng3011.vdtecomberefresh.config.SecurityConfig;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@ActiveProfiles("test")
@Import({ProfileControllerTest.ProfileControllerTestConfiguration.class, SecurityConfig.class})
public class ProfileControllerTest {

    @TestConfiguration
    static class ProfileControllerTestConfiguration {
        @Bean
        public ProfileService profileService() {
            return Mockito.mock(ProfileService.class);
        }
        
        @Bean
        public AuthProfileService authProfileService() {
            return Mockito.mock(AuthProfileService.class);
        }
        
        @Bean
        public com.hungng3011.vdtecomberefresh.profile.repository.ProfileRepository profileRepository() {
            return Mockito.mock(com.hungng3011.vdtecomberefresh.profile.repository.ProfileRepository.class);
        }
        
        @Bean
        public com.hungng3011.vdtecomberefresh.profile.mappers.ProfileMapper profileMapper() {
            return Mockito.mock(com.hungng3011.vdtecomberefresh.profile.mappers.ProfileMapper.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileService profileService; // Autowire the mock bean
    
    @Autowired
    private AuthProfileService authProfileService; // Autowire the mock bean

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

        when(authProfileService.syncProfileFromJwt(any(Jwt.class))).thenReturn(profileDto);

        mockMvc.perform(post("/v1/profiles/me/sync")
                        .with(jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void syncMyProfile_ShouldReturnUnauthorized_WhenNoJwt() throws Exception {
        mockMvc.perform(post("/v1/profiles/me/sync"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void syncMyProfile_ShouldReturnNotFound_WhenProfileDoesNotExist() throws Exception {
        Jwt mockJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .build();
                
        when(authProfileService.syncProfileFromJwt(any(Jwt.class)))
                .thenThrow(new EntityNotFoundException("Profile not found"));
                
        mockMvc.perform(post("/v1/profiles/me/sync")
                .with(jwt().jwt(mockJwt)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void syncMyProfile_ShouldReturnBadRequest_WhenJwtHasInvalidFormat() throws Exception {
        Jwt mockJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "invalid-uuid")
                .build();
                
        when(authProfileService.syncProfileFromJwt(any(Jwt.class)))
                .thenThrow(new IllegalArgumentException("Invalid UUID format"));
                
        mockMvc.perform(post("/v1/profiles/me/sync")
                .with(jwt().jwt(mockJwt)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProfiles_ShouldReturnPagedResponse_WhenSuccessful() throws Exception {
        // Create mock paginated response
        com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse<ProfileDto> mockResponse = 
            com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse.<ProfileDto>builder()
                .content(java.util.Arrays.asList(profileDto))
                .pagination(com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse.PaginationMetadata.builder()
                    .page(0)
                    .size(10)
                    .totalElements(1)
                    .totalPages(1)
                    .hasNext(false)
                    .hasPrevious(false)
                    .nextCursor(null)
                    .previousCursor(null)
                    .build())
                .build();
        
        when(profileService.getAllWithPagination(null, 10)).thenReturn(mockResponse);
        
        mockMvc.perform(get("/v1/profiles")
                .param("limit", "10")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_profile"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userId").value(profileDto.getUserId().toString()))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.size").value(10))
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.pagination.hasNext").value(false))
                .andExpect(jsonPath("$.pagination.hasPrevious").value(false));
    }

    @Test
    void getAllProfiles_ShouldReturnPagedResponse_WithCursor() throws Exception {
        // Create mock paginated response with cursor
        com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse<ProfileDto> mockResponse = 
            com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse.<ProfileDto>builder()
                .content(java.util.Arrays.asList(profileDto))
                .pagination(com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse.PaginationMetadata.builder()
                    .page(0)
                    .size(5)
                    .totalElements(10)
                    .totalPages(2)
                    .hasNext(true)
                    .hasPrevious(false)
                    .nextCursor(123L)
                    .previousCursor(null)
                    .build())
                .build();
        
        when(profileService.getAllWithPagination(null, 5)).thenReturn(mockResponse);
        
        mockMvc.perform(get("/v1/profiles")
                .param("limit", "5")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_profile"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userId").value(profileDto.getUserId().toString()))
                .andExpect(jsonPath("$.pagination.hasNext").value(true))
                .andExpect(jsonPath("$.pagination.nextCursor").value(123));
    }

    @Test
    void getAllProfiles_ShouldReturnPagedResponse_WithPreviousCursor() throws Exception {
        // Create mock paginated response for previous page
        com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse<ProfileDto> mockResponse = 
            com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse.<ProfileDto>builder()
                .content(java.util.Arrays.asList(profileDto))
                .pagination(com.hungng3011.vdtecomberefresh.common.dtos.PagedResponse.PaginationMetadata.builder()
                    .page(0)
                    .size(5)
                    .totalElements(10)
                    .totalPages(2)
                    .hasNext(true)
                    .hasPrevious(true)
                    .nextCursor(456L)
                    .previousCursor(123L)
                    .build())
                .build();
        
        when(profileService.getAllWithPreviousCursor(456L, 5)).thenReturn(mockResponse);
        
        mockMvc.perform(get("/v1/profiles")
                .param("cursor", "456")
                .param("limit", "5")
                .param("previous", "true")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_profile"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pagination.hasPrevious").value(true))
                .andExpect(jsonPath("$.pagination.previousCursor").value(123));
    }
}