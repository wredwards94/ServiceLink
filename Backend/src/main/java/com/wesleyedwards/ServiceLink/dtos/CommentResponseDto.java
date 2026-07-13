package com.wesleyedwards.ServiceLink.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponseDto(Long id,
                                 UUID authorId,
                                 String authorName,
                                 Long ticketId,
                                 String content,
                                 @JsonFormat(pattern = "MM/dd/yyyy hh:mm a")
                                 LocalDateTime createdAt,
                                 boolean internal) {
}
