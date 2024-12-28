package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.ProfileRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.ProfileResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileRequestDto requestDtoToEntity(Profile profile);
    ProfileResponseDto entityToResponseDto(Profile profile);
}
