package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;

import java.util.UUID;

public interface CommentService {
    CommentResponseDto addCommentToTicket(Long ticketId, UUID authorId, CommentRequestDto commentRequest);
}
