package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.JwtAuthFilter;
import com.wesleyedwards.ServiceLink.config.SecurityConfig;
import com.wesleyedwards.ServiceLink.dtos.AttachmentDownloadDto;
import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.service.AttachmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AttachmentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AttachmentController")
class AttachmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AttachmentService attachmentService;

    private final byte[] pngBytes = "fake-png-bytes".getBytes();

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "shot.png", "image/png", pngBytes);
    }

    private AttachmentResponseDto dto() {
        return new AttachmentResponseDto(
                42L, "shot.png", "image/png", pngBytes.length, UUID.randomUUID(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /ticket/{id} forwards the multipart part named \"file\" and returns 201")
    void uploadToTicket_created() throws Exception {
        when(attachmentService.uploadToTicket(eq(1L), any(), any())).thenReturn(dto());

        mockMvc.perform(multipart("/api/attachments/ticket/1").file(png()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.filename").value("shot.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"));

        verify(attachmentService).uploadToTicket(eq(1L), any(), any());
    }

    @Test
    @DisplayName("POST /comment/{id} returns 201")
    void uploadToComment_created() throws Exception {
        when(attachmentService.uploadToComment(eq(7L), any(), any())).thenReturn(dto());

        mockMvc.perform(multipart("/api/attachments/comment/7").file(png()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("shot.png"));

        verify(attachmentService).uploadToComment(eq(7L), any(), any());
    }

    @Test
    @DisplayName("POST surfaces a service 403 as Forbidden")
    void uploadToTicket_forbidden() throws Exception {
        when(attachmentService.uploadToTicket(eq(1L), any(), any()))
                .thenThrow(new ForbiddenException("You are not allowed to view this ticket"));

        mockMvc.perform(multipart("/api/attachments/ticket/1").file(png()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /ticket/{id} lists attachment metadata")
    void listForTicket_ok() throws Exception {
        when(attachmentService.listForTicket(eq(1L), any())).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/attachments/ticket/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].filename").value("shot.png"));
    }

    @Test
    @DisplayName("GET /comment/{id} lists attachment metadata")
    void listForComment_ok() throws Exception {
        when(attachmentService.listForComment(eq(7L), any())).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/attachments/comment/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("shot.png"));
    }

    @Test
    @DisplayName("GET /{id}/download returns the bytes with content type + disposition")
    void download_ok() throws Exception {
        when(attachmentService.downloadAttachment(eq(42L), any()))
                .thenReturn(new AttachmentDownloadDto("shot.png", "image/png", pngBytes));

        byte[] body = mockMvc.perform(get("/api/attachments/42/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shot.png\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(pngBytes, body);
    }

    @Test
    @DisplayName("GET /{id}/download surfaces a missing attachment as 404")
    void download_notFound() throws Exception {
        when(attachmentService.downloadAttachment(eq(42L), any()))
                .thenThrow(new NotFoundException("Attachment 42 not found."));

        mockMvc.perform(get("/api/attachments/42/download"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /{id} returns 204")
    void delete_noContent() throws Exception {
        mockMvc.perform(delete("/api/attachments/42"))
                .andExpect(status().isNoContent());

        verify(attachmentService).deleteAttachment(eq(42L), any());
    }
}
