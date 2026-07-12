package com.wesleyedwards.ServiceLink.mappers;

import com.wesleyedwards.ServiceLink.dtos.ProfileRequestDto;
import com.wesleyedwards.ServiceLink.dtos.ProfileResponseDto;
import com.wesleyedwards.ServiceLink.dtos.ProfileUpdateDto;
import com.wesleyedwards.ServiceLink.entities.Profile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileRequestDto requestDtoToEntity(Profile profile);
    ProfileResponseDto entityToResponseDto(Profile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromDto(ProfileUpdateDto dto, @MappingTarget Profile profile);
}
