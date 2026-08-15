package com.wesleyedwards.ServiceLink.mappers;

import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(target = "uploader", source = "uploader.userId")
    AttachmentResponseDto entityToResponseDto(Attachment attachment);
}
