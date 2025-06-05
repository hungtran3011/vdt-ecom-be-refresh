package com.hungng3011.vdtecomberefresh.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

/**
 * DTO for role assignment request
 */
@Data
public class RoleAssignmentDto {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Role name is required")
    private String roleName;
    
    private Set<String> roles;
}
