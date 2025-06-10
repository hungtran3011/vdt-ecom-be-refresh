package com.hungng3011.vdtecomberefresh.profile.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.profile.models.District;
import com.hungng3011.vdtecomberefresh.profile.models.Province;
import com.hungng3011.vdtecomberefresh.profile.models.Ward;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AddressService {

    private List<Province> provinces;

    @PostConstruct
    public void init() throws IOException {
        try {
            log.info("Initializing address service with divisions data");
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/divisions.json");
            if (is == null) {
                throw new IOException("Could not find divisions.json file in classpath");
            }
            provinces = Arrays.asList(mapper.readValue(is, Province[].class));
            log.info("Successfully loaded {} provinces from divisions data", provinces.size());
        } catch (Exception e) {
            log.error("Error initializing address service", e);
            throw e;
        }
    }

    public List<Province> getAllProvinces() {
        try {
            log.info("Retrieving all provinces");
            log.info("Returned {} provinces", provinces.size());
            return provinces;
        } catch (Exception e) {
            log.error("Error retrieving all provinces", e);
            throw e;
        }
    }

    public List<District> getDistrictsByProvinceCode(int provinceCode) {
        try {
            log.info("Retrieving districts for province code: {}", provinceCode);
            List<District> districts = provinces.stream()
                    .filter(p -> p.getCode() == provinceCode)
                    .findFirst()
                    .map(Province::getDistricts)
                    .orElse(Collections.emptyList());
            
            if (districts.isEmpty()) {
                log.warn("No districts found for province code: {}", provinceCode);
            } else {
                log.info("Found {} districts for province code: {}", districts.size(), provinceCode);
            }
            return districts;
        } catch (Exception e) {
            log.error("Error retrieving districts for province code: {}", provinceCode, e);
            throw e;
        }
    }

    public List<Ward> getWardsByDistrictCode(int districtCode) {
        try {
            log.info("Retrieving wards for district code: {}", districtCode);
            List<Ward> wards = provinces.stream()
                    .flatMap(p -> p.getDistricts().stream())
                    .filter(d -> d.getCode() == districtCode)
                    .findFirst()
                    .map(District::getWards)
                    .orElse(Collections.emptyList());
                    
            if (wards.isEmpty()) {
                log.warn("No wards found for district code: {}", districtCode);
            } else {
                log.info("Found {} wards for district code: {}", wards.size(), districtCode);
            }
            return wards;
        } catch (Exception e) {
            log.error("Error retrieving wards for district code: {}", districtCode, e);
            throw e;
        }
    }

    public Optional<Province> findProvinceByCode(int code) {
        try {
            log.info("Finding province by code: {}", code);
            Optional<Province> province = provinces.stream().filter(p -> p.getCode() == code).findFirst();
            if (province.isPresent()) {
                log.info("Found province: {} with code: {}", province.get().getName(), code);
            } else {
                log.warn("Province not found with code: {}", code);
            }
            return province;
        } catch (Exception e) {
            log.error("Error finding province by code: {}", code, e);
            throw e;
        }
    }

    public Optional<District> findDistrictByCode(int code) {
        try {
            log.info("Finding district by code: {}", code);
            Optional<District> district = provinces.stream()
                    .flatMap(p -> p.getDistricts().stream())
                    .filter(d -> d.getCode() == code)
                    .findFirst();
            if (district.isPresent()) {
                log.info("Found district: {} with code: {}", district.get().getName(), code);
            } else {
                log.warn("District not found with code: {}", code);
            }
            return district;
        } catch (Exception e) {
            log.error("Error finding district by code: {}", code, e);
            throw e;
        }
    }

    public Optional<Ward> findWardByCode(int code) {
        try {
            log.info("Finding ward by code: {}", code);
            Optional<Ward> ward = provinces.stream()
                    .flatMap(p -> p.getDistricts().stream())
                    .flatMap(d -> d.getWards().stream())
                    .filter(w -> w.getCode() == code)
                    .findFirst();
            if (ward.isPresent()) {
                log.info("Found ward: {} with code: {}", ward.get().getName(), code);
            } else {
                log.warn("Ward not found with code: {}", code);
            }
            return ward;
        } catch (Exception e) {
            log.error("Error finding ward by code: {}", code, e);
            throw e;
        }
    }
}

