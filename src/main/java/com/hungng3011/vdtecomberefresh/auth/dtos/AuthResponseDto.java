package com.hungng3011.vdtecomberefresh.auth.dtos;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    
    private String userId;
    private String username;
    private String email;
    private String message;
    private boolean success;
}
