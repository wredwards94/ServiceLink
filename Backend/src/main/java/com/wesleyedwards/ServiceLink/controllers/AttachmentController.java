package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.AttachmentDownloadDto;
import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import com.wesleyedwards.ServiceLink.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    @PostMapping("/ticket/{ticketId}")
    public ResponseEntity<AttachmentResponseDto> uploadAttachmentToTicket(@RequestParam("file") MultipartFile file,
                                                                          @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @PathVariable Long ticketId) {
        AttachmentResponseDto uploaded = attachmentService.uploadToTicket(ticketId, file, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    @GetMapping("/ticket/{ticketId}")
    public List<AttachmentResponseDto> getAttachmentsForTicket(@PathVariable Long ticketId,
                                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return attachmentService.listForTicket(ticketId, userPrincipal);
    }

    @PostMapping("/comment/{commentId}")
    public ResponseEntity<AttachmentResponseDto> uploadAttachmentToComment(@RequestParam("file") MultipartFile file,
                                                                           @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                           @PathVariable Long commentId) {
        AttachmentResponseDto uploaded = attachmentService.uploadToComment(commentId, file, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    @GetMapping("/comment/{commentId}")
    public List<AttachmentResponseDto> getAttachmentsForComment(@PathVariable Long commentId,
                                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return attachmentService.listForComment(commentId, userPrincipal);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId,
                                                     @AuthenticationPrincipal UserPrincipal userPrincipal) {
        AttachmentDownloadDto attachment = attachmentService.downloadAttachment(attachmentId, userPrincipal);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.filename() + "\"")
                .body(attachment.data());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId,
                                                 @AuthenticationPrincipal UserPrincipal userPrincipal) {
        attachmentService.deleteAttachment(attachmentId, userPrincipal);
        return ResponseEntity.noContent().build();
    }
}
