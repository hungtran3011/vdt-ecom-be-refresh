package com.hungng3011.vdtecomberefresh.profile.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class District {
    private String name;
    private Integer code;
    private String codename;
    private String division_type;
    private List<Ward> wards;

    private String short_codename;
}
