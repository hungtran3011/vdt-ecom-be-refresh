package com.hungng3011.vdtecomberefresh.auth.services;

import com.hungng3011.vdtecomberefresh.auth.dto.UserRegistrationDto;
import com.hungng3011.vdtecomberefresh.auth.dto.PasswordResetDto;
import com.hungng3011.vdtecomberefresh.auth.dto.RoleAssignmentDto;
import com.hungng3011.vdtecomberefresh.exception.profile.ProfileProcessingException;
import com.hungng3011.vdtecomberefresh.auth.services.AuthProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * Service for managing Keycloak users and roles.
 * This service handles backend-initiated user management operations using the Keycloak Admin API.
 * 
 * Note: In PKCE flow, frontend applications authenticate directly with Keycloak.
 * This service is for administrative operations only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final Keycloak keycloak;
    private final AuthProfileService authProfileService;

    @Value("${app.keycloak.realm}")
    private String realm;

    /**
     * Create a new user in Keycloak using UserRegistrationDto
     */
    @Transactional
    public String createUser(UserRegistrationDto registrationDto) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(registrationDto.getUsername());
            user.setEmail(registrationDto.getEmail());
            user.setFirstName(registrationDto.getFirstName());
            user.setLastName(registrationDto.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(registrationDto.isEmailVerified());

            // Create user
            Response response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                String userId = extractUserIdFromResponse(response);
                
                // Set password
                setUserPassword(userId, registrationDto.getPassword(), registrationDto.isTemporaryPassword());
                
                // Assign default CUSTOMER role if no roles specified
                if (registrationDto.getRoles() == null || registrationDto.getRoles().isEmpty()) {
                    assignRoleToUser(userId, "CUSTOMER");
                } else {
                    // Assign specified roles
                    for (String role : registrationDto.getRoles()) {
                        assignRoleToUser(userId, role);
                    }
                }
                
                // Create corresponding profile in the application database
                authProfileService.createProfileForKeycloakUser(
                    userId, 
                    registrationDto.getEmail(),
                    registrationDto.getFirstName(),
                    registrationDto.getLastName()
                );
                
                log.info("User created successfully: {}", registrationDto.getUsername());
                return userId;
            } else {
                throw new ProfileProcessingException(
                    "USER_CREATION_FAILED", 
                    "Failed to create user. Status: " + response.getStatus()
                );
            }
        } catch (Exception e) {
            log.error("Error creating user: {}", registrationDto.getUsername(), e);
            throw new ProfileProcessingException(
                "USER_CREATION_ERROR", 
                "Failed to create user: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Update user information
     * Only updates fields that are provided (non-null)
     */
    @Transactional
    public void updateUser(String userId, String email, String firstName, String lastName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            UserRepresentation user = userResource.toRepresentation();
            if (user == null) {
                throw new ProfileProcessingException("USER_NOT_FOUND", "User not found with ID: " + userId);
            }

            // Only update fields that are provided (non-null)
            if (email != null) {
                user.setEmail(email);
            }
            if (firstName != null) {
                user.setFirstName(firstName);
            }
            if (lastName != null) {
                user.setLastName(lastName);
            }

            userResource.update(user);
            log.info("User updated successfully: {}", userId);
            
            // Synchronize changes with the profile database
            UserRepresentation updatedUser = userResource.toRepresentation();
            authProfileService.updateProfileFromKeycloakUser(
                userId, 
                updatedUser.getEmail(), 
                updatedUser.getFirstName(), 
                updatedUser.getLastName()
            );
        } catch (Exception e) {
            log.error("Error updating user: {}", userId, e);
            throw new ProfileProcessingException(
                "USER_UPDATE_ERROR", 
                "Failed to update user: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Delete user from Keycloak
     */
    public void deleteUser(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            Response response = realmResource.users().delete(userId);
            
            if (response.getStatus() != 204) {
                throw new ProfileProcessingException(
                    "USER_DELETE_FAILED", 
                    "Failed to delete user. Status: " + response.getStatus()
                );
            }
            
            log.info("User deleted successfully: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new ProfileProcessingException(
                "USER_DELETE_ERROR", 
                "Failed to delete user: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Assign role to user using RoleAssignmentDto
     */
    public void assignRoleToUser(RoleAssignmentDto roleAssignment) {
        assignRoleToUser(roleAssignment.getUserId(), roleAssignment.getRoleName());
    }

    /**
     * Assign role to user
     */
    public void assignRoleToUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Verify user exists
            UserRepresentation user = userResource.toRepresentation();
            if (user == null) {
                throw new ProfileProcessingException("USER_NOT_FOUND", "User not found with ID: " + userId);
            }

            // Get role representation
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            if (role == null) {
                throw new ProfileProcessingException("ROLE_NOT_FOUND", "Role not found: " + roleName);
            }
            
            // Assign role to user
            userResource.roles().realmLevel().add(Arrays.asList(role));
            log.info("Role '{}' assigned to user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error assigning role '{}' to user: {}", roleName, userId, e);
            throw new ProfileProcessingException(
                "ROLE_ASSIGNMENT_ERROR", 
                "Failed to assign role: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Remove role from user
     */
    public void removeRoleFromUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Verify user exists
            UserRepresentation user = userResource.toRepresentation();
            if (user == null) {
                throw new ProfileProcessingException("USER_NOT_FOUND", "User not found with ID: " + userId);
            }

            // Get role representation
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            if (role == null) {
                throw new ProfileProcessingException("ROLE_NOT_FOUND", "Role not found: " + roleName);
            }
            
            // Remove role from user
            userResource.roles().realmLevel().remove(Arrays.asList(role));
            log.info("Role '{}' removed from user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error removing role '{}' from user: {}", roleName, userId, e);
            throw new ProfileProcessingException(
                "ROLE_REMOVAL_ERROR", 
                "Failed to remove role: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Get user by ID
     */
    public UserRepresentation getUser(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            if (user == null) {
                throw new ProfileProcessingException("USER_NOT_FOUND", "User not found with ID: " + userId);
            }
            return user;
        } catch (Exception e) {
            log.error("Error getting user: {}", userId, e);
            throw new ProfileProcessingException(
                "USER_RETRIEVAL_ERROR", 
                "Failed to get user: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Reset user password using PasswordResetDto
     */
    public void resetUserPassword(PasswordResetDto passwordReset) {
        try {
            setUserPassword(passwordReset.getUserId(), passwordReset.getNewPassword(), passwordReset.isTemporary());
            log.info("Password reset successfully for user: {}", passwordReset.getUserId());
        } catch (Exception e) {
            log.error("Error resetting password for user: {}", passwordReset.getUserId(), e);
            throw new ProfileProcessingException(
                "PASSWORD_RESET_ERROR", 
                "Failed to reset password: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Enable/disable user
     */
    public void setUserEnabled(String userId, boolean enabled) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            UserRepresentation user = userResource.toRepresentation();
            if (user == null) {
                throw new ProfileProcessingException("USER_NOT_FOUND", "User not found with ID: " + userId);
            }

            user.setEnabled(enabled);
            userResource.update(user);
            
            log.info("User {} {}: {}", enabled ? "enabled" : "disabled", "successfully", userId);
        } catch (Exception e) {
            log.error("Error {} user: {}", enabled ? "enabling" : "disabling", userId, e);
            throw new ProfileProcessingException(
                "USER_STATUS_UPDATE_ERROR", 
                "Failed to " + (enabled ? "enable" : "disable") + " user: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Get user roles
     */
    public List<RoleRepresentation> getUserRoles(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            
            // Verify user exists
            UserRepresentation user = userResource.toRepresentation();
            if (user == null) {
                throw new ProfileProcessingException("USER_NOT_FOUND", "User not found with ID: " + userId);
            }
            
            return userResource.roles().realmLevel().listAll();
        } catch (Exception e) {
            log.error("Error getting user roles: {}", userId, e);
            throw new ProfileProcessingException(
                "ROLE_RETRIEVAL_ERROR", 
                "Failed to get user roles: " + e.getMessage(),
                HttpStatus.BAD_REQUEST,
                e
            );
        }
    }

    /**
     * Private helper method to set user password
     */
    private void setUserPassword(String userId, String password, boolean temporary) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(temporary);

        userResource.resetPassword(credential);
    }

    /**
     * Private helper method to extract user ID from response
     */
    private String extractUserIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        if (location != null) {
            String[] pathSegments = location.split("/");
            return pathSegments[pathSegments.length - 1];
        }
        throw new ProfileProcessingException(
            "USER_ID_EXTRACTION_ERROR", 
            "Failed to extract user ID from response"
        );
    }
}
