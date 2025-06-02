package com.hungng3011.vdtecomberefresh.profile.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.profile.models.District;
import com.hungng3011.vdtecomberefresh.profile.models.Province;
import com.hungng3011.vdtecomberefresh.profile.models.Ward;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private List<Province> provinces;

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/divisions.json");
        provinces = Arrays.asList(mapper.readValue(is, Province[].class));
    }

    public List<Province> getAllProvinces() {
        return provinces;
    }

    public List<District> getDistrictsByProvinceCode(int provinceCode) {
        return provinces.stream()
                .filter(p -> p.getCode() == provinceCode)
                .findFirst()
                .map(Province::getDistricts)
                .orElse(Collections.emptyList());
    }

    public List<Ward> getWardsByDistrictCode(int districtCode) {
        return provinces.stream()
                .flatMap(p -> p.getDistricts().stream())
                .filter(d -> d.getCode() == districtCode)
                .findFirst()
                .map(District::getWards)
                .orElse(Collections.emptyList());
    }

    public Optional<Province> findProvinceByCode(int code) {
        return provinces.stream().filter(p -> p.getCode() == code).findFirst();
    }

    public Optional<District> findDistrictByCode(int code) {
        return provinces.stream()
                .flatMap(p -> p.getDistricts().stream())
                .filter(d -> d.getCode() == code)
                .findFirst();
    }

    public Optional<Ward> findWardByCode(int code) {
        return provinces.stream()
                .flatMap(p -> p.getDistricts().stream())
                .flatMap(d -> d.getWards().stream())
                .filter(w -> w.getCode() == code)
                .findFirst();
    }
}

