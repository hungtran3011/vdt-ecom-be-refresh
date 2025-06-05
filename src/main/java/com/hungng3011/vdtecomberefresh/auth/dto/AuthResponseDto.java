package com.hungng3011.vdtecomberefresh.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for authentication response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private boolean enabled;
    private boolean emailVerified;
    private String message;
    
    public AuthResponseDto(String message) {
        this.message = message;
    }
}
