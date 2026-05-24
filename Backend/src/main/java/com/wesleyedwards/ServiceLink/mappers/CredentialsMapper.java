package com.wesleyedwards.ServiceLink.mappers;

import com.wesleyedwards.ServiceLink.dtos.CredentialsRequestDto;
import com.wesleyedwards.ServiceLink.entities.Credentials;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CredentialsMapper {
    CredentialsRequestDto requestDtoToEntity(Credentials credentials);
}
