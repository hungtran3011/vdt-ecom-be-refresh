package com.hungng3011.vdtecomberefresh.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for updating user information.
 * Contains validation rules to ensure data integrity.
 */
public record UserUpdateDto(
    @Schema(description = "User's email address", example = "user@example.com")
    @Email(message = "Please provide a valid email address")
    String email,
    
    @Schema(description = "User's first name", example = "John")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[^<>'\"/;`%]*$", message = "First name contains invalid characters")
    String firstName,
    
    @Schema(description = "User's last name", example = "Doe")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[^<>'\"/;`%]*$", message = "Last name contains invalid characters")
    String lastName
) {}
