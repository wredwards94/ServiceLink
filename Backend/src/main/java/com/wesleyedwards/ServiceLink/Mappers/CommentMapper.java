package com.wesleyedwards.ServiceLink.Mappers;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Comment;
import com.wesleyedwards.ServiceLink.Entities.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = { TicketMapper.class })
public interface CommentMapper {

    Comment requestDtoToEntity(CommentRequestDto commentRequestDto);

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorName", expression = "java(getFullName(comment.getAuthor()))")
    CommentResponseDto entityToResponseDto(Comment comment);

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorName", expression = "java(getFullName(comment.getAuthor()))")
    List<CommentResponseDto> entitiesToResponseDtos(List<Comment> comments);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    void updateCommentFromDto(CommentRequestDto dto, @MappingTarget Comment comment);

    default String getFullName(User user) {
        return user.getProfile().getFirstName() + " " + user.getProfile().getLastName();
    }
}
