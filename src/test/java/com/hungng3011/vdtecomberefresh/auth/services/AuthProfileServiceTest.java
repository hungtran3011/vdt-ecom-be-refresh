package com.hungng3011.vdtecomberefresh.auth.services;

import com.hungng3011.vdtecomberefresh.exception.profile.ProfileProcessingException;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthProfileServiceTest {

    @Mock
    private Keycloak keycloak;
    
    @Mock
    private RealmResource realmResource;
    
    @Mock
    private UsersResource usersResource;
    
    @Mock
    private UserResource userResource;
    
    @Mock
    private UserRepresentation userRepresentation;
    
    @Mock
    private com.hungng3011.vdtecomberefresh.profile.services.ProfileService profileService;

    @InjectMocks
    private AuthProfileService authProfileService;
    
    private UUID userId;
    private ProfileDto profileDto;
    private final String REALM = "test-realm";

    @BeforeEach
    void setUp() {
        // Setup test data
        userId = UUID.randomUUID();
        profileDto = new ProfileDto();
        profileDto.setUserId(userId);
        profileDto.setEmail("updated@example.com");
        profileDto.setFullName("John Smith");
        
        // Set the realm value using reflection
        ReflectionTestUtils.setField(authProfileService, "realm", REALM);
        
        // Configure mocks
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
    }

    @Test
    void updateKeycloakFromProfile_ShouldUpdateKeycloakUser_WhenProfileChanged() {
        // Arrange
        when(userRepresentation.getEmail()).thenReturn("old@example.com");
        when(userRepresentation.getFirstName()).thenReturn("Old");
        when(userRepresentation.getLastName()).thenReturn("Name");
        
        // Act
        boolean result = authProfileService.updateKeycloakFromProfile(profileDto);
        
        // Assert
        assertTrue(result);
        verify(userRepresentation).setEmail("updated@example.com");
        verify(userRepresentation).setFirstName("John");
        verify(userRepresentation).setLastName("Smith");
        verify(userResource).update(userRepresentation);
    }

    @Test
    void updateKeycloakFromProfile_ShouldNotUpdateKeycloak_WhenNoChanges() {
        // Arrange
        when(userRepresentation.getEmail()).thenReturn("updated@example.com");
        when(userRepresentation.getFirstName()).thenReturn("John");
        when(userRepresentation.getLastName()).thenReturn("Smith");
        
        // Act
        boolean result = authProfileService.updateKeycloakFromProfile(profileDto);
        
        // Assert
        assertFalse(result);
        verify(userResource, never()).update(any(UserRepresentation.class));
    }
    
    @Test
    void updateKeycloakFromProfile_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userResource.toRepresentation()).thenReturn(null);
        
        // Act & Assert
        assertThrows(ProfileProcessingException.class, () -> 
            authProfileService.updateKeycloakFromProfile(profileDto)
        );
    }
    
    @Test
    void updateKeycloakFromProfile_ShouldThrowException_WhenProfileIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            authProfileService.updateKeycloakFromProfile(null)
        );
    }

    @Test
    void createProfileForKeycloakUser_shouldCreateAndReturnProfileDto() {
        // Arrange
        String userIdStr = UUID.randomUUID().toString();
        String email = "test@example.com";
        String firstName = "Test";
        String lastName = "User";
        
        ProfileDto expectedProfileDto = new ProfileDto();
        expectedProfileDto.setEmail(email);
        expectedProfileDto.setFullName("Test User");
        
        when(profileService.createOrUpdate(any(ProfileDto.class))).thenReturn(expectedProfileDto);
        
        // Act
        ProfileDto result = authProfileService.createProfileForKeycloakUser(
            userIdStr, email, firstName, lastName
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals("Test User", result.getFullName());
        verify(profileService, times(1)).createOrUpdate(any(ProfileDto.class));
    }
    
    @Test
    void updateProfileFromKeycloakUser_shouldUpdateExistingProfile() {
        // Arrange
        String userIdStr = userId.toString();
        String email = "updated@example.com";
        String firstName = "Updated";
        String lastName = "User";
        
        ProfileDto existingProfile = new ProfileDto();
        existingProfile.setUserId(userId);
        existingProfile.setEmail("old@example.com");
        existingProfile.setFullName("Old User");
        
        ProfileDto updatedProfile = new ProfileDto();
        updatedProfile.setUserId(userId);
        updatedProfile.setEmail(email);
        updatedProfile.setFullName("Updated User");
        
        when(profileService.getProfile(userId)).thenReturn(existingProfile);
        when(profileService.createOrUpdate(any(ProfileDto.class))).thenReturn(updatedProfile);
        
        // Act
        ProfileDto result = authProfileService.updateProfileFromKeycloakUser(
            userIdStr, email, firstName, lastName
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals("Updated User", result.getFullName());
        verify(profileService, times(1)).getProfile(userId);
        verify(profileService, times(1)).createOrUpdate(any(ProfileDto.class));
    }
    
    @Test
    void syncProfileFromJwt_shouldSyncProfileFromJwtClaims() {
        // Arrange
        org.springframework.security.oauth2.jwt.Jwt jwt = mock(org.springframework.security.oauth2.jwt.Jwt.class);
        
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwt.getClaimAsString("email")).thenReturn("jwt@example.com");
        when(jwt.getClaimAsString("given_name")).thenReturn("JWT");
        when(jwt.getClaimAsString("family_name")).thenReturn("User");
        
        ProfileDto existingProfile = new ProfileDto();
        existingProfile.setUserId(userId);
        existingProfile.setEmail("old@example.com");
        
        ProfileDto updatedProfile = new ProfileDto();
        updatedProfile.setUserId(userId);
        updatedProfile.setEmail("jwt@example.com");
        updatedProfile.setFullName("JWT User");
        
        when(profileService.getProfile(userId)).thenReturn(existingProfile);
        when(profileService.createOrUpdate(any(ProfileDto.class))).thenReturn(updatedProfile);
        
        // Act
        ProfileDto result = authProfileService.syncProfileFromJwt(jwt);
        
        // Assert
        assertNotNull(result);
        assertEquals("jwt@example.com", result.getEmail());
        assertEquals("JWT User", result.getFullName());
        verify(profileService, times(1)).getProfile(userId);
        verify(profileService, times(1)).createOrUpdate(any(ProfileDto.class));
    }
    
    @Test
    void syncProfileFromJwt_shouldCreateNewProfileIfNotExist() {
        // Arrange
        org.springframework.security.oauth2.jwt.Jwt jwt = mock(org.springframework.security.oauth2.jwt.Jwt.class);
        
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwt.getClaimAsString("email")).thenReturn("jwt@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("JWT User");
        
        when(profileService.getProfile(userId)).thenThrow(new jakarta.persistence.EntityNotFoundException());
        
        ProfileDto createdProfile = new ProfileDto();
        createdProfile.setUserId(userId);
        createdProfile.setEmail("jwt@example.com");
        createdProfile.setFullName("JWT User");
        
        when(profileService.createOrUpdate(any(ProfileDto.class))).thenReturn(createdProfile);
        
        // Act
        ProfileDto result = authProfileService.syncProfileFromJwt(jwt);
        
        // Assert
        assertNotNull(result);
        assertEquals("jwt@example.com", result.getEmail());
        assertEquals("JWT User", result.getFullName());
        verify(profileService, times(1)).getProfile(userId);
        verify(profileService, times(1)).createOrUpdate(any(ProfileDto.class));
    }
}
