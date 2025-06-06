package com.hungng3011.vdtecomberefresh.auth.services;

import com.hungng3011.vdtecomberefresh.exception.profile.ProfileProcessingException;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service to handle bidirectional synchronization between Keycloak user data and the application's profile data.
 * This service helps ensure data consistency when profile updates happen on either side.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthProfileService {

    private final Keycloak keycloak;
    private final ProfileService profileService;

    @Value("${app.keycloak.realm}")
    private String realm;

    /**
     * Updates a user's information in Keycloak based on profile changes in the application.
     * This method should be called whenever user profile data is updated through the application.
     *
     * @param profileDto The updated profile data
     * @return Whether the update was successful
     * @throws ProfileProcessingException If the update fails
     */
    @Transactional
    public boolean updateKeycloakFromProfile(ProfileDto profileDto) {
        if (profileDto == null || profileDto.getUserId() == null) {
            throw new IllegalArgumentException("Profile and userId must not be null");
        }

        String userId = profileDto.getUserId().toString();

        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            UserRepresentation user = userResource.toRepresentation();
            if (user == null) {
                log.error("User not found in Keycloak with ID: {}", userId);
                throw new ProfileProcessingException(
                        "USER_NOT_FOUND",
                        "User not found in Keycloak with ID: " + userId,
                        HttpStatus.NOT_FOUND
                );
            }

            boolean updated = false;

            // Update email if it has changed
            if (profileDto.getEmail() != null && !profileDto.getEmail().equals(user.getEmail())) {
                user.setEmail(profileDto.getEmail());
                updated = true;
            }

            // Handle full name - we need to try to split it into firstName and lastName
            if (profileDto.getFullName() != null) {
                Map<String, String> nameMap = splitFullName(profileDto.getFullName());
                
                // Update firstName if it has changed and is different
                String firstName = nameMap.get("firstName");
                if (firstName != null && !firstName.equals(user.getFirstName())) {
                    user.setFirstName(firstName);
                    updated = true;
                }
                
                // Update lastName if it has changed and is different
                String lastName = nameMap.get("lastName");
                if (lastName != null && !lastName.equals(user.getLastName())) {
                    user.setLastName(lastName);
                    updated = true;
                }
            }

            // If any fields were updated, send the update to Keycloak
            if (updated) {
                userResource.update(user);
                log.info("Keycloak user updated from profile: {}", userId);
            } else {
                log.debug("No changes detected for Keycloak user: {}", userId);
            }

            return updated;
            
        } catch (ProfileProcessingException e) {
            // Re-throw ProfileProcessingExceptions
            throw e;
        } catch (Exception e) {
            log.error("Error updating Keycloak user from profile: {}", userId, e);
            throw new ProfileProcessingException(
                    "KEYCLOAK_UPDATE_ERROR",
                    "Failed to update Keycloak user: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    /**
     * Create a profile for a new user that has been registered in Keycloak
     *
     * @param userId Keycloak user ID
     * @param email User's email
     * @param firstName User's first name
     * @param lastName User's last name
     * @return The created ProfileDto
     */
    @Transactional
    public ProfileDto createProfileForKeycloakUser(String userId, String email, String firstName, String lastName) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Create new profile
            ProfileDto profileDto = new ProfileDto();
            profileDto.setUserId(userUuid);
            profileDto.setEmail(email);
            
            // Set full name if we have first or last name
            String fullName = formatFullName(firstName, lastName);
            if (fullName != null && !fullName.isEmpty()) {
                profileDto.setFullName(fullName);
            }
            
            // Save profile
            return profileService.createOrUpdate(profileDto);
        } catch (Exception e) {
            log.error("Failed to create profile for Keycloak user: {}", userId, e);
            throw new ProfileProcessingException(
                "PROFILE_CREATION_ERROR",
                "Failed to create profile for Keycloak user: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        }
    }
    
    /**
     * Update a profile after the corresponding Keycloak user has been updated
     *
     * @param userId Keycloak user ID
     * @param email Updated email (may be null)
     * @param firstName Updated first name (may be null)
     * @param lastName Updated last name (may be null)
     * @return The updated ProfileDto
     */
    @Transactional
    public ProfileDto updateProfileFromKeycloakUser(String userId, String email, String firstName, String lastName) {
        try {
            UUID userUuid = UUID.fromString(userId);
            ProfileDto profileDto;
            
            try {
                // Try to get existing profile
                profileDto = profileService.getProfile(userUuid);
            } catch (EntityNotFoundException e) {
                // If profile doesn't exist, create a new one
                return createProfileForKeycloakUser(userId, email, firstName, lastName);
            }

            // Update only non-null fields
            if (email != null) {
                profileDto.setEmail(email);
            }
            
            // Update full name if either first or last name was provided
            if (firstName != null || lastName != null) {
                // Get current first/last names to preserve parts that aren't being updated
                String currentFirstName = firstName;
                String currentLastName = lastName;
                
                // If we don't have a name part that's being updated, try to extract it from existing full name
                if (profileDto.getFullName() != null) {
                    if (firstName == null) {
                        currentFirstName = extractFirstName(profileDto.getFullName());
                    }
                    if (lastName == null) {
                        currentLastName = extractLastName(profileDto.getFullName());
                    }
                }
                
                String fullName = formatFullName(currentFirstName, currentLastName);
                profileDto.setFullName(fullName);
            }
            
            // Save updated profile
            return profileService.createOrUpdate(profileDto);
        } catch (Exception e) {
            log.error("Failed to update profile from Keycloak user: {}", userId, e);
            throw new ProfileProcessingException(
                "PROFILE_UPDATE_ERROR",
                "Failed to update profile from Keycloak user: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        }
    }
    
    /**
     * Update a profile using data from a JWT token
     * 
     * @param jwt The JWT token containing user information
     * @return The updated ProfileDto
     */
    @Transactional
    public ProfileDto syncProfileFromJwt(Jwt jwt) {
        String userIdString = jwt.getSubject();
        if (userIdString == null) {
            throw new IllegalArgumentException("JWT 'sub' claim is missing. Cannot sync profile.");
        }
        
        UUID userId;
        try {
            userId = UUID.fromString(userIdString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format for 'sub' claim: " + userIdString);
        }
        
        // Extract data from JWT
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");
        
        ProfileDto profileDto;
        try {
            // Try to get existing profile
            profileDto = profileService.getProfile(userId);
        } catch (EntityNotFoundException e) {
            // Create new profile if it doesn't exist
            profileDto = new ProfileDto();
            profileDto.setUserId(userId);
        }

        // Update email if provided in the JWT
        if (email != null) {
            profileDto.setEmail(email);
        }
        
        // Figure out the full name
        if (name != null) {
            // If JWT provides a ready-made full name, use it
            profileDto.setFullName(name);
        } else if (firstName != null || lastName != null) {
            // Otherwise construct from first/last name parts
            profileDto.setFullName(formatFullName(firstName, lastName));
        }
        
        // Save the updated profile
        return profileService.createOrUpdate(profileDto);
    }

    /**
     * Split a full name into first name and last name components
     *
     * @param fullName The full name to split
     * @return Map containing "firstName" and "lastName" keys
     */
    private Map<String, String> splitFullName(String fullName) {
        Map<String, String> nameMap = new HashMap<>();
        
        if (fullName == null || fullName.trim().isEmpty()) {
            return nameMap;
        }

        String[] parts = fullName.trim().split("\\s+");
        
        if (parts.length > 0) {
            nameMap.put("firstName", parts[0]);
            
            if (parts.length > 1) {
                // If there are multiple parts, the last part is the last name
                nameMap.put("lastName", parts[parts.length - 1]);
                
                // For more sophisticated name parsing in the future, you could:
                // 1. Handle middle names
                // 2. Handle cultures with different name ordering
                // 3. Use a name parsing library
            }
        }
        
        return nameMap;
    }
    
    /**
     * Format first and last name into a full name
     */
    private String formatFullName(String firstName, String lastName) {
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        
        return fullName.length() > 0 ? fullName.toString() : null;
    }
    
    /**
     * Extract first name from a full name
     * Simple implementation - in a real system this would be more sophisticated
     */
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }
    
    /**
     * Extract last name from a full name
     * Simple implementation - in a real system this would be more sophisticated
     */
    private String extractLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : null;
    }
}