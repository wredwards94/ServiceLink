package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Comment;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.CommentMapper;
import com.wesleyedwards.ServiceLink.repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.CommentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;


    @Override
    @Transactional
    public CommentResponseDto addCommentToTicket(Long ticketId, UUID authorId, CommentRequestDto commentRequest) {
        Ticket foundTicket = checkTicketExists(ticketId);
        User foundUser = checkUserExists(authorId);
        Comment newComment = commentMapper.requestDtoToEntity(commentRequest);

        newComment.setTicket(foundTicket);
        newComment.setAuthor(foundUser);

        commentRepository.saveAndFlush(newComment);

        ticketRepository.saveAndFlush(foundTicket);

        return commentMapper.entityToResponseDto(newComment);
    }

    @Override
    public List<CommentResponseDto> getCommentsForTicket(Long ticketId) {
        checkTicketExists(ticketId);

        return commentMapper.entitiesToResponseDtos(commentRepository.findAllByTicketId(ticketId));
    }

    @Override
    public void deleteComment(Long commentId) {
        checkCommentExists(commentId);

        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto updatedComment) {
        Comment foundComment = checkCommentExists(commentId);

        commentMapper.updateCommentFromDto(updatedComment, foundComment);

        return commentMapper.entityToResponseDto(commentRepository.saveAndFlush(foundComment));
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new NotFoundException("Ticket " + id + " not found.");

//        System.out.println(optionalTicket.get().getAssignedTo().getUserId());

        return optionalTicket.get();
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
