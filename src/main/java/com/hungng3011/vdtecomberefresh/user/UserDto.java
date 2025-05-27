package com.hungng3011.vdtecomberefresh.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
public class UserDto {
    private @Getter @Setter Long id;
    private @Getter @Setter String email;
    private @Getter @Setter LocalDate birthday;
    private @Getter @Setter String fullName;
    private @Getter @Setter String avatarUrl;
}
