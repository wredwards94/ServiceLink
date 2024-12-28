package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Credentials;
import com.wesleyedwards.ServiceLink.Entities.Profile;
import com.wesleyedwards.ServiceLink.Entities.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = { CredentialsMapper.class, ProfileMapper.class})
public interface UserMapper {

    User requestDtoToEntity(UserRequestDto userRequestDto);
    UserResponseDto entityToResponseDto(User user);
    List<UserResponseDto> entitiesToResponseDtos(List<User> users);
}
