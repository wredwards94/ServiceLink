package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.ProfileRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.ProfileResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Profile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileRequestDto requestDtoToEntity(Profile profile);
    ProfileResponseDto entityToResponseDto(Profile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromDto(ProfileRequestDto dto, @MappingTarget Profile profile);
}
