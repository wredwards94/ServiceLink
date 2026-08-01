package com.wesleyedwards.ServiceLink.mappers;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TicketMapper.class, CommentMapper.class})
public interface AttachmentMapper {
}
