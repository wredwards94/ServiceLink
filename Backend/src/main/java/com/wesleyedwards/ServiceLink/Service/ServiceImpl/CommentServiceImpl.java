package com.wesleyedwards.ServiceLink.Service.ServiceImpl;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Comment;
import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Entities.User;
import com.wesleyedwards.ServiceLink.Exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.Mappers.CommentMapper;
import com.wesleyedwards.ServiceLink.Repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.Repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.Repositories.UserRepository;
import com.wesleyedwards.ServiceLink.Service.CommentService;
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
    public CommentResponseDto addCommentToTicket(Long ticketId, UUID authorId, CommentRequestDto commentRequest) {
        Ticket foundTicket = checkTicketExists(ticketId);
        User foundUser = checkUserExists(authorId);
        Comment newComment = commentMapper.requestDtoToEntity(commentRequest);

        newComment.setTicket(foundTicket);
        newComment.setAuthorId(foundUser.getUserId());

        foundTicket.getComments().add(newComment);
        commentRepository.saveAndFlush(newComment);
        foundTicket.setUpdatedAt(newComment.getCreatedAt());
        ticketRepository.saveAndFlush(foundTicket);

        return commentMapper.entityToResponseDto(newComment);
    }

    @Override
    public List<CommentResponseDto> getCommentsForTicket(Long ticketId) {
        checkTicketExists(ticketId);

        return commentMapper.entitiesToResponseDtos(commentRepository.findAllByTicketId(ticketId));
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new NotFoundException("Ticket " + id + " not found");

//        System.out.println(optionalTicket.get().getAssignedTo().getUserId());

        return optionalTicket.get();
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist");

        return optionalUser.get();
    }
}
