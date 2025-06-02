package com.hungng3011.vdtecomberefresh.profile.entities;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Address {
    private Integer provinceCode;
    private String provinceName;

    private Integer districtCode;
    private String districtName;

    private Integer wardCode;
    private String wardName;

    private String detailed; // e.g., house number and street
}
