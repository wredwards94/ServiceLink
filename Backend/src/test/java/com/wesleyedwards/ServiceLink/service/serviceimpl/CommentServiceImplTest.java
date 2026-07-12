package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Comment;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.CommentMapper;
import com.wesleyedwards.ServiceLink.repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentMapper commentMapper;

    @InjectMocks private CommentServiceImpl commentService;

    private Ticket ticket;
    private User user;
    private Comment comment;
    private CommentResponseDto commentDto;
    private final Long ticketId = 1L;
    private final Long commentId = 7L;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        ticket = new Ticket();
        ticket.setId(ticketId);
        user = new User();
        user.setUserId(authorId);
        comment = new Comment();
        comment.setId(commentId);
        comment.setContent("Please advise");
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now()); // within the edit window by default
        commentDto = new CommentResponseDto(commentId, authorId, "John Doe", ticketId, "Please advise", null);
    }

    private UserPrincipal principal(UUID id, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        return new UserPrincipal(u);
    }

    @Test
    @DisplayName("addCommentToTicket links the ticket and author then saves")
    void addCommentToTicket_links() {
        CommentRequestDto request = new CommentRequestDto("Please advise");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(user));
        when(commentMapper.requestDtoToEntity(request)).thenReturn(comment);
        when(commentMapper.entityToResponseDto(comment)).thenReturn(commentDto);

        CommentResponseDto result = commentService.addCommentToTicket(ticketId, authorId, request);

        assertSame(commentDto, result);
        assertSame(ticket, comment.getTicket());
        assertSame(user, comment.getAuthor());
        verify(commentRepository).saveAndFlush(comment);
    }

    @Test
    @DisplayName("addCommentToTicket throws when the ticket does not exist")
    void addCommentToTicket_ticketMissing() {
        CommentRequestDto request = new CommentRequestDto("Please advise");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.addCommentToTicket(ticketId, authorId, request));
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("addCommentToTicket throws when the author does not exist")
    void addCommentToTicket_authorMissing() {
        CommentRequestDto request = new CommentRequestDto("Please advise");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.addCommentToTicket(ticketId, authorId, request));
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("getCommentsForTicket returns mapped comments for an existing ticket")
    void getCommentsForTicket_returnsComments() {
        List<Comment> comments = List.of(comment);
        List<CommentResponseDto> dtos = List.of(commentDto);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findAllByTicketId(ticketId)).thenReturn(comments);
        when(commentMapper.entitiesToResponseDtos(comments)).thenReturn(dtos);

        assertSame(dtos, commentService.getCommentsForTicket(ticketId));
    }

    @Test
    @DisplayName("getCommentsForTicket throws when the ticket does not exist")
    void getCommentsForTicket_ticketMissing() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.getCommentsForTicket(ticketId));
        verify(commentRepository, never()).findAllByTicketId(anyLong());
    }

    @Test
    @DisplayName("deleteComment removes an existing comment")
    void deleteComment_deletes() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId);

        verify(commentRepository).deleteById(commentId);
    }

    @Test
    @DisplayName("deleteComment throws and does not delete when missing")
    void deleteComment_missing() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.deleteComment(commentId));
        verify(commentRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("updateComment lets the author edit within the window and saves")
    void updateComment_authorWithinWindow() {
        CommentRequestDto update = new CommentRequestDto("Updated content");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.saveAndFlush(comment)).thenReturn(comment);
        when(commentMapper.entityToResponseDto(comment)).thenReturn(commentDto);

        assertSame(commentDto,
                commentService.updateComment(commentId, update, principal(authorId, Role.USER)));
        verify(commentMapper).updateCommentFromDto(update, comment);
        verify(commentRepository).saveAndFlush(comment);
    }

    @Test
    @DisplayName("updateComment forbids the author once the edit window has passed")
    void updateComment_windowExpired() {
        comment.setCreatedAt(LocalDateTime.now().minus(Duration.ofMinutes(20)));
        CommentRequestDto update = new CommentRequestDto("Updated content");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> commentService.updateComment(commentId, update, principal(authorId, Role.USER)));
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateComment forbids a USER who is not the author")
    void updateComment_notAuthorForbidden() {
        CommentRequestDto update = new CommentRequestDto("Updated content");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> commentService.updateComment(commentId, update, principal(UUID.randomUUID(), Role.USER)));
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateComment lets staff edit any comment regardless of the window")
    void updateComment_staffBypassesWindow() {
        comment.setCreatedAt(LocalDateTime.now().minus(Duration.ofMinutes(20)));
        CommentRequestDto update = new CommentRequestDto("Updated content");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.saveAndFlush(comment)).thenReturn(comment);
        when(commentMapper.entityToResponseDto(comment)).thenReturn(commentDto);

        assertSame(commentDto,
                commentService.updateComment(commentId, update, principal(UUID.randomUUID(), Role.AGENT)));
        verify(commentRepository).saveAndFlush(comment);
    }

    @Test
    @DisplayName("updateComment throws when the comment does not exist")
    void updateComment_missing() {
        CommentRequestDto update = new CommentRequestDto("Updated content");
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.updateComment(commentId, update, principal(authorId, Role.USER)));
        verify(commentRepository, never()).saveAndFlush(any());
    }
}
