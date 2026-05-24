package com.wesleyedwards.ServiceLink.mappers;

import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import org.mapstruct.*;

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

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "requester", ignore = true)
    void updateTicketFromDto(TicketRequestDto dto, @MappingTarget Ticket ticket);
}
