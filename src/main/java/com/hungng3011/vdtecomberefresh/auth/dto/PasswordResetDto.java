package com.hungng3011.vdtecomberefresh.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for password reset request
 */
@Data
public class PasswordResetDto {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
    
    private boolean temporary = false;
}
