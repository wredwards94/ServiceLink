package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = { TicketMapper.class })
public interface CommentMapper {

    Comment requestDtoToEntity(CommentRequestDto commentRequestDto);

    @Mapping(target = "ticketId", source = "ticket.id")
    CommentResponseDto entityToResponseDto(Comment comment);

    @Mapping(target = "ticketId", source = "ticket.id")
    List<CommentResponseDto> entitiesToResponseDtos(List<Comment> comments);
}
