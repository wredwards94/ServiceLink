package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.AttachmentDownloadDto;
import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Attachment;
import com.wesleyedwards.ServiceLink.entities.Comment;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.AttachmentMapper;
import com.wesleyedwards.ServiceLink.repositories.AttachmentRepository;
import com.wesleyedwards.ServiceLink.repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.TicketAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentServiceImpl")
class AttachmentServiceImplTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AttachmentMapper attachmentMapper;

    @InjectMocks private AttachmentServiceImpl attachmentService;
    @Spy private TicketAccessPolicy ticketAccessPolicy = new TicketAccessPolicyImpl();

    private Ticket ticket;
    private Comment comment;
    private User requester;
    private UUID requesterId;
    private AttachmentResponseDto responseDto;

    private final Long ticketId = 1L;
    private final Long commentId = 7L;
    private final Long attachmentId = 42L;
    private final byte[] pngBytes = "fake-png-bytes".getBytes();

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        requester = new User();
        requester.setUserId(requesterId);

        ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setRequester(requester);

        comment = new Comment();
        comment.setId(commentId);
        comment.setTicket(ticket);

        responseDto = new AttachmentResponseDto(
                attachmentId, "shot.png", "image/png", pngBytes.length, requesterId, LocalDateTime.now());
    }

    private UserPrincipal principal(UUID id, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        return new UserPrincipal(u);
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "shot.png", "image/png", pngBytes);
    }

    private Attachment storedAttachment() {
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setFilename("shot.png");
        attachment.setContentType("image/png");
        attachment.setSize(pngBytes.length);
        attachment.setData(pngBytes);
        attachment.setUploader(requester);
        return attachment;
    }

    // ---------- uploadToTicket ----------

    @Test
    @DisplayName("uploadToTicket copies the multipart metadata + bytes onto the entity and links the ticket")
    void uploadToTicket_persistsFile() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(attachmentRepository.saveAndFlush(any(Attachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attachmentMapper.entityToResponseDto(any(Attachment.class)))
                .thenReturn(responseDto);

        AttachmentResponseDto result =
                attachmentService.uploadToTicket(ticketId, png(), principal(requesterId, Role.USER));

        assertSame(responseDto, result);

        ArgumentCaptor<Attachment> saved = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).saveAndFlush(saved.capture());
        Attachment attachment = saved.getValue();
        assertEquals("shot.png", attachment.getFilename());
        assertEquals("image/png", attachment.getContentType());
        assertEquals(pngBytes.length, attachment.getSize());
        assertArrayEquals(pngBytes, attachment.getData());
        assertSame(ticket, attachment.getTicket());
        assertSame(requester, attachment.getUploader());
    }

    @Test
    @DisplayName("uploadToTicket rejects a USER who does not own the ticket")
    void uploadToTicket_forbiddenForNonOwner() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(ForbiddenException.class,
                () -> attachmentService.uploadToTicket(ticketId, png(), principal(UUID.randomUUID(), Role.USER)));

        verify(attachmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("uploadToTicket 404s for a missing ticket")
    void uploadToTicket_missingTicket() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> attachmentService.uploadToTicket(ticketId, png(), principal(requesterId, Role.USER)));
    }

    @Test
    @DisplayName("uploadToTicket rejects an empty file")
    void uploadToTicket_rejectsEmpty() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThrows(BadRequestException.class,
                () -> attachmentService.uploadToTicket(ticketId, empty, principal(requesterId, Role.USER)));
    }

    @Test
    @DisplayName("uploadToTicket rejects a file over the 10MB cap")
    void uploadToTicket_rejectsOversize() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        MockMultipartFile huge =
                new MockMultipartFile("file", "huge.png", "image/png", new byte[10 * 1024 * 1024 + 1]);

        assertThrows(BadRequestException.class,
                () -> attachmentService.uploadToTicket(ticketId, huge, principal(requesterId, Role.USER)));
    }

    @Test
    @DisplayName("uploadToTicket rejects a content type outside the allowlist")
    void uploadToTicket_rejectsDisallowedType() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        MockMultipartFile exe =
                new MockMultipartFile("file", "evil.exe", "application/x-msdownload", new byte[] {1, 2, 3});

        assertThrows(BadRequestException.class,
                () -> attachmentService.uploadToTicket(ticketId, exe, principal(requesterId, Role.USER)));
    }

    // ---------- uploadToComment ----------

    @Test
    @DisplayName("uploadToComment links the comment")
    void uploadToComment_persistsFile() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(attachmentRepository.saveAndFlush(any(Attachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attachmentMapper.entityToResponseDto(any(Attachment.class)))
                .thenReturn(responseDto);

        attachmentService.uploadToComment(commentId, png(), principal(requesterId, Role.USER));

        ArgumentCaptor<Attachment> saved = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).saveAndFlush(saved.capture());
        assertSame(comment, saved.getValue().getComment());
    }

    @Test
    @DisplayName("uploadToComment blocks a non-staff user on an internal comment")
    void uploadToComment_internalIsStaffOnly() {
        comment.setInternal(true);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // the requester owns the ticket, so only the internal rule can reject them
        assertThrows(ForbiddenException.class,
                () -> attachmentService.uploadToComment(commentId, png(), principal(requesterId, Role.USER)));

        verify(attachmentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("uploadToComment allows staff on an internal comment")
    void uploadToComment_internalAllowedForStaff() {
        UUID agentId = UUID.randomUUID();
        User agent = new User();
        agent.setUserId(agentId);
        comment.setInternal(true);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(attachmentRepository.saveAndFlush(any(Attachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attachmentMapper.entityToResponseDto(any(Attachment.class)))
                .thenReturn(responseDto);

        attachmentService.uploadToComment(commentId, png(), principal(agentId, Role.AGENT));

        verify(attachmentRepository).saveAndFlush(any(Attachment.class));
    }

    // ---------- listing ----------

    @Test
    @DisplayName("listForTicket returns metadata for the ticket owner")
    void listForTicket_returnsMetadata() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(attachmentRepository.listForTicket(ticketId)).thenReturn(List.of(responseDto));

        List<AttachmentResponseDto> result =
                attachmentService.listForTicket(ticketId, principal(requesterId, Role.USER));

        assertEquals(1, result.size());
        assertSame(responseDto, result.get(0));
    }

    @Test
    @DisplayName("listForTicket rejects a USER who does not own the ticket")
    void listForTicket_forbiddenForNonOwner() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(ForbiddenException.class,
                () -> attachmentService.listForTicket(ticketId, principal(UUID.randomUUID(), Role.USER)));

        verify(attachmentRepository, never()).listForTicket(ticketId);
    }

    @Test
    @DisplayName("listForComment blocks a non-staff user on an internal comment")
    void listForComment_internalIsStaffOnly() {
        comment.setInternal(true);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenException.class,
                () -> attachmentService.listForComment(commentId, principal(requesterId, Role.USER)));

        verify(attachmentRepository, never()).listForComment(commentId);
    }

    // ---------- download ----------

    @Test
    @DisplayName("downloadAttachment returns the bytes with filename + content type")
    void downloadAttachment_returnsBytes() {
        Attachment stored = storedAttachment();
        stored.setTicket(ticket);
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(stored));

        AttachmentDownloadDto result =
                attachmentService.downloadAttachment(attachmentId, principal(requesterId, Role.USER));

        assertEquals("shot.png", result.filename());
        assertEquals("image/png", result.contentType());
        assertArrayEquals(pngBytes, result.data());
    }

    @Test
    @DisplayName("downloadAttachment gates a comment attachment on the internal rule")
    void downloadAttachment_internalCommentIsStaffOnly() {
        comment.setInternal(true);
        Attachment stored = storedAttachment();
        stored.setComment(comment);
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(stored));

        assertThrows(ForbiddenException.class,
                () -> attachmentService.downloadAttachment(attachmentId, principal(requesterId, Role.USER)));
    }

    @Test
    @DisplayName("downloadAttachment 404s for a missing attachment")
    void downloadAttachment_missing() {
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> attachmentService.downloadAttachment(attachmentId, principal(requesterId, Role.USER)));
    }

    // ---------- delete ----------

    @Test
    @DisplayName("deleteAttachment soft-deletes for staff")
    void deleteAttachment_staff() {
        Attachment stored = storedAttachment();
        stored.setTicket(ticket);
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(stored));

        attachmentService.deleteAttachment(attachmentId, principal(UUID.randomUUID(), Role.AGENT));

        verify(attachmentRepository).deleteById(attachmentId);
    }

    @Test
    @DisplayName("deleteAttachment rejects a non-staff user")
    void deleteAttachment_forbiddenForUser() {
        Attachment stored = storedAttachment();
        stored.setTicket(ticket);
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(stored));

        assertThrows(ForbiddenException.class,
                () -> attachmentService.deleteAttachment(attachmentId, principal(requesterId, Role.USER)));

        verify(attachmentRepository, never()).deleteById(attachmentId);
    }
}
