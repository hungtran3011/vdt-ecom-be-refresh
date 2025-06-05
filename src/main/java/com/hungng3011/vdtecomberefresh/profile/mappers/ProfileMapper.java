package com.hungng3011.vdtecomberefresh.profile.mappers;

import com.hungng3011.vdtecomberefresh.profile.dtos.AddressDto;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import com.hungng3011.vdtecomberefresh.profile.entities.Address;
import com.hungng3011.vdtecomberefresh.profile.entities.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileDto toDto(Profile profile);

    Profile toEntity(ProfileDto profileDto);

    AddressDto toDto(Address address);

    Address toEntity(AddressDto addressDto);

    default ProfileDto updateEntityFromDto(Profile entity) {
        if (entity == null) {
            return null;
        }
        ProfileDto dto = toDto(entity);
        toEntity(dto);
        return dto;
    }
}