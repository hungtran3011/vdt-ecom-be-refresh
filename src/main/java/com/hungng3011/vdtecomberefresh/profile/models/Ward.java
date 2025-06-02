package com.hungng3011.vdtecomberefresh.profile.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ward {
    private String name;
    private Integer code;
    private String codename;
    private String division_type;
    private String short_codename;
}