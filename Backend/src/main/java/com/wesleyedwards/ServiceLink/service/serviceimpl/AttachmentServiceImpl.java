package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.AttachmentDownloadDto;
import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Attachment;
import com.wesleyedwards.ServiceLink.entities.Comment;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.AttachmentMapper;
import com.wesleyedwards.ServiceLink.repositories.AttachmentRepository;
import com.wesleyedwards.ServiceLink.repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.AttachmentService;
import com.wesleyedwards.ServiceLink.service.TicketAccessPolicy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AttachmentMapper attachmentMapper;
    private final TicketAccessPolicy ticketAccess;

    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10 MB, matches spring.servlet.multipart limits
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf", "text/plain", "text/csv");

    @Override
    @Transactional
    public AttachmentResponseDto uploadToTicket(Long ticketId, MultipartFile file, UserPrincipal actor) {
        Ticket foundTicket = checkTicketExists(ticketId);
        ticketAccess.assertCanView(actor, foundTicket);

        Attachment attachment = buildAttachment(file, checkUserExists(actor.getUserId()));
        attachment.setTicket(foundTicket);

        return attachmentMapper.entityToResponseDto(attachmentRepository.saveAndFlush(attachment));
    }

    @Override
    @Transactional
    public AttachmentResponseDto uploadToComment(Long commentId, MultipartFile file, UserPrincipal actor) {
        Comment foundComment = checkCommentExists(commentId);
        assertCanAccessComment(actor, foundComment);

        Attachment attachment = buildAttachment(file, checkUserExists(actor.getUserId()));
        attachment.setComment(foundComment);

        return attachmentMapper.entityToResponseDto(attachmentRepository.saveAndFlush(attachment));
    }

    @Override
    @Transactional
    public List<AttachmentResponseDto> listForTicket(Long ticketId, UserPrincipal actor) {
        ticketAccess.assertCanView(actor, checkTicketExists(ticketId));

        return attachmentRepository.listForTicket(ticketId);
    }

    @Override
    @Transactional
    public List<AttachmentResponseDto> listForComment(Long commentId, UserPrincipal actor) {
        assertCanAccessComment(actor, checkCommentExists(commentId));

        return attachmentRepository.listForComment(commentId);
    }

    @Override
    @Transactional
    public AttachmentDownloadDto downloadAttachment(Long attachmentId, UserPrincipal actor) {
        Attachment foundAttachment = checkAttachmentExists(attachmentId);

        if (foundAttachment.getComment() != null) {
            assertCanAccessComment(actor, foundAttachment.getComment());
        } else {
            ticketAccess.assertCanView(actor, foundAttachment.getTicket());
        }

        return new AttachmentDownloadDto(
                foundAttachment.getFilename(), foundAttachment.getContentType(), foundAttachment.getData());
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, UserPrincipal actor) {
        checkAttachmentExists(attachmentId);

        if (!actor.isStaff()) throw new ForbiddenException("You are not allowed to delete this attachment");

        attachmentRepository.deleteById(attachmentId);
    }

    private Attachment buildAttachment(MultipartFile file, User uploader) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Attachment file is empty.");

        if (file.getSize() > MAX_SIZE)
            throw new BadRequestException("Attachment exceeds the maximum size of 10MB.");

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType))
            throw new BadRequestException("Attachment type " + contentType + " is not allowed.");

        Attachment attachment = new Attachment();
        attachment.setFilename(file.getOriginalFilename());
        attachment.setContentType(contentType);
        attachment.setSize(file.getSize());
        attachment.setUploader(uploader);

        try {
            attachment.setData(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file.");
        }

        return attachment;
    }

    // An internal comment is invisible to non-staff, so they may not attach to it, list it, or download from it.
    private void assertCanAccessComment(UserPrincipal actor, Comment comment) {
        ticketAccess.assertCanView(actor, comment.getTicket());

        if (comment.isInternal() && !actor.isStaff())
            throw new ForbiddenException("You are not allowed to view this comment");
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new NotFoundException("Ticket " + id + " not found.");

        return optionalTicket.get();
    }

    private Comment checkCommentExists(Long id) {
        Optional<Comment> optionalComment = commentRepository.findById(id);

        if(optionalComment.isEmpty()) throw new NotFoundException("This comment does not exists.");

        return optionalComment.get();
    }

    private Attachment checkAttachmentExists(Long id) {
        Optional<Attachment> optionalAttachment = attachmentRepository.findById(id);

        if(optionalAttachment.isEmpty()) throw new NotFoundException("Attachment " + id + " not found.");

        return optionalAttachment.get();
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist.");

        return optionalUser.get();
    }

}
