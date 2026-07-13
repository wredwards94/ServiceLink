package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Comment;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.CommentMapper;
import com.wesleyedwards.ServiceLink.repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.CommentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private static final Duration EDIT_WINDOW = Duration.ofMinutes(15);


    @Override
    @Transactional
    public CommentResponseDto addCommentToTicket(Long ticketId, UserPrincipal actor, CommentRequestDto commentRequest) {
        Ticket foundTicket = checkTicketExists(ticketId);
        User foundUser = checkUserExists(actor.getUserId());
        Comment newComment = commentMapper.requestDtoToEntity(commentRequest);

        newComment.setTicket(foundTicket);
        newComment.setAuthor(foundUser);
        newComment.setInternal(actor.isStaff() && commentRequest.internal());

        commentRepository.saveAndFlush(newComment);

        return commentMapper.entityToResponseDto(newComment);
    }

    @Override
    public Page<CommentResponseDto> getCommentsForTicket(Long ticketId,  Pageable pageable, UserPrincipal actor) {
        assertCanView(actor, checkTicketExists(ticketId));

        return actor.isStaff() ? commentRepository.findAllByTicketId(ticketId, pageable).map(commentMapper::entityToResponseDto)
                : commentRepository.findAllByTicketIdAndInternalFalse(ticketId, pageable).map(commentMapper::entityToResponseDto);
    }

    @Override
    public void deleteComment(Long commentId) {
        checkCommentExists(commentId);

        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto updatedComment, UserPrincipal actor) {
        Comment foundComment = checkCommentExists(commentId);

        // author can edit their own within the window; staff can edit anytime
        if (!actor.isStaff()) {
            if (!foundComment.getAuthor().getUserId().equals(actor.getUserId()))
                throw new ForbiddenException("You can only edit your own comments");
            if (foundComment.getCreatedAt().isBefore(LocalDateTime.now().minus(EDIT_WINDOW)))
                throw new ForbiddenException("The edit window for this comment has passed");
        }

        commentMapper.updateCommentFromDto(updatedComment, foundComment);

        return commentMapper.entityToResponseDto(commentRepository.saveAndFlush(foundComment));
    }

    @Override
    public Page<CommentResponseDto> searchComments(Long ticketId, String keyword, Pageable pageable, UserPrincipal actor) {
        assertCanView(actor, checkTicketExists(ticketId));
        return actor.isStaff() ?
                commentRepository.searchByTicketAndKeyword(ticketId, keyword, pageable).map(commentMapper::entityToResponseDto)
                : commentRepository.searchByTicketAndKeywordAndInternalFalse(ticketId, keyword, pageable).map(commentMapper::entityToResponseDto);
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new NotFoundException("Ticket " + id + " not found.");

        return optionalTicket.get();
    }
    private void assertCanView(UserPrincipal actor, Ticket ticket) {
        if (actor.isStaff()) return;
        boolean isRequester = ticket.getRequester() != null
                && ticket.getRequester().getUserId().equals(actor.getUserId());
        if (!isRequester) {
            throw new ForbiddenException("You are not allowed to view this ticket");
        }
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist.");

        return optionalUser.get();
    }

    private Comment checkCommentExists(Long id) {
        Optional<Comment> optionalComment = commentRepository.findById(id);

        if(optionalComment.isEmpty()) throw new NotFoundException("This comment does not exists.");

        return optionalComment.get();
    }


}
