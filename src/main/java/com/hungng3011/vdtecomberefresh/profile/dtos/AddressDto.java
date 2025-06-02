package com.hungng3011.vdtecomberefresh.profile.dtos;

import lombok.Data;

@Data
public class AddressDto {
    private Integer provinceCode;
    private Integer districtCode;
    private Integer wardCode;
    private String detailed; // e.g., house number and street
}
