package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {
    CommentResponseDto addCommentToTicket(Long ticketId, UserPrincipal actor, CommentRequestDto commentRequest);

    Page<CommentResponseDto> getCommentsForTicket(Long ticketId, Pageable pageable, UserPrincipal actor);

    void deleteComment(Long commentId);

    CommentResponseDto updateComment(Long commentId, CommentRequestDto updatedComment, UserPrincipal actor);

    Page<CommentResponseDto> searchComments(Long ticketId, String keyword, Pageable pageable, UserPrincipal actor);
}
