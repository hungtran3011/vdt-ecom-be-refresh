package com.hungng3011.vdtecomberefresh.profile.dtos;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProfileDto {
    private UUID userId;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private AddressDto address;
}
