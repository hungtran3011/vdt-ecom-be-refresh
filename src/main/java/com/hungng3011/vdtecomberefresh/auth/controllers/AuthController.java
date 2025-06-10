package com.hungng3011.vdtecomberefresh.auth.controllers;

import com.hungng3011.vdtecomberefresh.auth.dto.UserRegistrationDto;
import com.hungng3011.vdtecomberefresh.auth.dto.PasswordResetDto;
import com.hungng3011.vdtecomberefresh.auth.dto.RoleAssignmentDto;
import com.hungng3011.vdtecomberefresh.auth.dto.UserUpdateDto;
import com.hungng3011.vdtecomberefresh.auth.services.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for authentication and user management operations.
 * 
 * IMPORTANT: This controller is for ADMIN operations only.
 * In PKCE flow:
 * 1. Frontend applications authenticate directly with Keycloak
 * 2. JWTs are validated by Spring Security
 * 3. This controller provides admin endpoints for user management
 * 
 * Authentication flow:
 * - Frontend → Keycloak (PKCE) → JWT
 * - Backend validates JWT and provides protected resources
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User management and authentication operations")
@SecurityRequirement(name = "bearer-jwt")
public class AuthController {

    private final UserManagementService userManagementService;

    /**
     * Get current user information from JWT token
     * This endpoint demonstrates how to extract user info from the PKCE flow JWT
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information from JWT")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = Map.of(
            "userId", jwt.getSubject(),
            "username", jwt.getClaimAsString("preferred_username"),
            "email", jwt.getClaimAsString("email"),
            "firstName", jwt.getClaimAsString("given_name"),
            "lastName", jwt.getClaimAsString("family_name"),
            "roles", jwt.getClaimAsStringList("realm_access.roles")
        );
        // log.info););
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Create a new user (ADMIN only)
     */
    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user", description = "Admin operation to create a new user in Keycloak")
    public ResponseEntity<Map<String, String>> createUser(@Valid @RequestBody UserRegistrationDto userRegistration) {
        String userId = userManagementService.createUser(userRegistration);
        
        Map<String, String> response = Map.of(
            "message", "User created successfully",
            "userId", userId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user by ID (ADMIN only)
     */
    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Admin operation to retrieve user information")
    public ResponseEntity<UserRepresentation> getUser(@PathVariable String userId) {
        UserRepresentation user = userManagementService.getUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user information (ADMIN only)
     */
    @PutMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user information", description = "Admin operation to update user details")
    public ResponseEntity<Map<String, String>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateDto updateDto) {
        
        userManagementService.updateUser(
            userId,
            updateDto.email(),
            updateDto.firstName(),
            updateDto.lastName()
        );
        
        Map<String, String> response = Map.of("message", "User updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user (ADMIN only)
     */
    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Admin operation to delete a user")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String userId) {
        userManagementService.deleteUser(userId);
        
        Map<String, String> response = Map.of("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Assign role to user (ADMIN only)
     */
    @PostMapping("/admin/users/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user", description = "Admin operation to assign a role to a user")
    public ResponseEntity<Map<String, String>> assignRole(@Valid @RequestBody RoleAssignmentDto roleAssignment) {
        userManagementService.assignRoleToUser(roleAssignment);
        
        Map<String, String> response = Map.of("message", "Role assigned successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Remove role from user (ADMIN only)
     */
    @DeleteMapping("/admin/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role from user", description = "Admin operation to remove a role from a user")
    public ResponseEntity<Map<String, String>> removeRole(
            @PathVariable String userId,
            @PathVariable String roleName) {
        
        userManagementService.removeRoleFromUser(userId, roleName);
        
        Map<String, String> response = Map.of("message", "Role removed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get user roles (ADMIN only)
     */
    @GetMapping("/admin/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user roles", description = "Admin operation to retrieve all roles assigned to a user")
    public ResponseEntity<List<RoleRepresentation>> getUserRoles(@PathVariable String userId) {
        List<RoleRepresentation> roles = userManagementService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    /**
     * Reset user password (ADMIN only)
     */
    @PostMapping("/admin/users/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset user password", description = "Admin operation to reset a user's password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetDto passwordReset) {
        userManagementService.resetUserPassword(passwordReset);
        
        Map<String, String> response = Map.of("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Enable/disable user (ADMIN only)
     */
    @PutMapping("/admin/users/{userId}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable user", description = "Admin operation to enable or disable a user account")
    public ResponseEntity<Map<String, String>> setUserEnabled(
            @PathVariable String userId,
            @RequestBody Map<String, Boolean> enabledData) {
        
        boolean enabled = enabledData.getOrDefault("enabled", true);
        userManagementService.setUserEnabled(userId, enabled);
        
        Map<String, String> response = Map.of(
            "message", "User " + (enabled ? "enabled" : "disabled") + " successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for authentication service
     */
    @GetMapping("/health")
    @Operation(summary = "Authentication service health check")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "authentication",
            "timestamp", java.time.Instant.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}
