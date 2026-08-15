package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.AttachmentDownloadDto;
import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {
    AttachmentResponseDto uploadToTicket(Long ticketId, MultipartFile file, UserPrincipal actor);
    AttachmentResponseDto uploadToComment(Long commentId, MultipartFile file, UserPrincipal actor);
    List<AttachmentResponseDto> listForTicket(Long ticketId, UserPrincipal actor);
    List<AttachmentResponseDto> listForComment(Long commentId, UserPrincipal actor);
    AttachmentDownloadDto downloadAttachment(Long attachmentId, UserPrincipal actor);
    void deleteAttachment(Long attachmentId, UserPrincipal actor);
}
