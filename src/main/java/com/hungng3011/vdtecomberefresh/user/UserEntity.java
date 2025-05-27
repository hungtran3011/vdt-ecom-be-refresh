package com.hungng3011.vdtecomberefresh.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.Period;


@Entity
public class UserEntity {
    @Id
    @SequenceGenerator(
            name = "user_generator",
            allocationSize=1
    )

    private @Getter Long id;
    @Column(unique = true, nullable = false)
    @Email
    private @Getter @Setter String email;
    private @Getter @Setter String fullName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private @Getter LocalDate dob;
    private @Getter @Setter String avatarUrl;

    public UserEntity() {

    }

    public UserEntity(
            String email,
            String fullName,
            LocalDate dob,
            String avatarUrl
    ) {
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        setDob(dob);
    }

    public void setDob(LocalDate dob) {
        Period age = Period.between(dob, LocalDate.now());
        if (age.getYears() >= 15) {
            this.dob = dob;
        }
        else {
            throw new RuntimeException("Not enough age");
        }
    }
}
