package com.wesleyedwards.ServiceLink.mappers;

import com.wesleyedwards.ServiceLink.dtos.UserIdResponseDto;
import com.wesleyedwards.ServiceLink.dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = { CredentialsMapper.class, ProfileMapper.class, TicketMapper.class })
public interface UserMapper {

    User requestDtoToEntity(UserRequestDto userRequestDto);
    UserResponseDto entityToResponseDto(User user);

    @Mapping(target = "token", ignore = true)
    UserIdResponseDto entityToIdResponseDto(User user);

    List<UserResponseDto> entitiesToResponseDtos(List<User> users);
}