package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CommentMapper.class})
public interface TicketMapper {

    Ticket requestDtoToEntity(TicketRequestDto ticketRequestDto);

    @Mapping(target = "assignedTo", source = "assignedTo.userId")
    @Mapping(target = "requester", source = "requester.userId")
    TicketResponseDto entityToResponseDto(Ticket ticket);

    @Mapping(target = "assignedTo", source = "assignedTo.userId")
    @Mapping(target = "requester", source = "requester.userId")
    List<TicketResponseDto> entitiesToResponseDtos(List<Ticket> tickets);
}
