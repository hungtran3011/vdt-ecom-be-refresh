package com.hungng3011.vdtecomberefresh.profile.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Province {
    private String name;
    private Integer code;
    private String codename;
    private String division_type;
    private Integer phone_code;
    private List<District> districts;
    private String short_codename;
}
