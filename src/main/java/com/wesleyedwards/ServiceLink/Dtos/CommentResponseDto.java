package com.wesleyedwards.ServiceLink.Dtos;

import java.util.UUID;

public record CommentResponseDto(Long id, UUID authorId, Long ticketId, String content, String createdAt) {
}
