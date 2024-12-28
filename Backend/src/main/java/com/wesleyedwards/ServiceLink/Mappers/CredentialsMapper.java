package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.CredentialsRequestDto;
import com.wesleyedwards.ServiceLink.Entities.Credentials;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CredentialsMapper {
    CredentialsRequestDto requestDtoToEntity(Credentials credentials);
}
