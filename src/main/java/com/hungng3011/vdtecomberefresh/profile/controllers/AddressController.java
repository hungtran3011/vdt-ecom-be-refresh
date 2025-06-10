package com.hungng3011.vdtecomberefresh.profile.controllers;

import com.hungng3011.vdtecomberefresh.profile.models.District;
import com.hungng3011.vdtecomberefresh.profile.models.Province;
import com.hungng3011.vdtecomberefresh.profile.models.Ward;
import com.hungng3011.vdtecomberefresh.profile.services.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/address")
@Slf4j
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/provinces")
    public List<Province> getAllProvinces() {
        log.info("Fetching all provinces");
        try {
            List<Province> provinces = addressService.getAllProvinces();
            log.info("Successfully retrieved {} provinces", provinces.size());
            return provinces;
        } catch (Exception e) {
            log.error("Error fetching provinces", e);
            throw e;
        }
    }

    @GetMapping("/provinces/{provinceCode}/districts")
    public List<District> getDistricts(@PathVariable int provinceCode) {
        log.info("Fetching districts for province code: {}", provinceCode);
        try {
            List<District> districts = addressService.getDistrictsByProvinceCode(provinceCode);
            log.info("Successfully retrieved {} districts for province code: {}", districts.size(), provinceCode);
            return districts;
        } catch (Exception e) {
            log.error("Error fetching districts for province code: {}", provinceCode, e);
            throw e;
        }
    }

    @GetMapping("/districts/{districtCode}/wards")
    public List<Ward> getWards(@PathVariable int districtCode) {
        return addressService.getWardsByDistrictCode(districtCode);
    }
}

