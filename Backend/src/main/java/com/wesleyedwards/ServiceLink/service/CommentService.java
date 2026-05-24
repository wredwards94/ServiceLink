package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;

import java.util.List;
import java.util.UUID;

public interface CommentService {
    CommentResponseDto addCommentToTicket(Long ticketId, UUID authorId, CommentRequestDto commentRequest);

    List<CommentResponseDto> getCommentsForTicket(Long ticketId);

    void deleteComment(Long commentId);

    CommentResponseDto updateComment(Long commentId, CommentRequestDto updatedComment);
}
