package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.JwtAuthFilter;
import com.wesleyedwards.ServiceLink.config.SecurityConfig;
import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.service.CommentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommentController")
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CommentService commentService;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private CommentResponseDto sampleComment(Long id, Long ticketId, UUID authorId) {
        return new CommentResponseDto(id, authorId, "John Doe", ticketId, "Please advise", null);
    }

    @Test
    @DisplayName("POST /api/comments/ticket/{ticketId} returns 201 using the authenticated user")
    void addComment_returns201() throws Exception {
        UUID authorId = UUID.randomUUID();

        User u = new User();
        u.setUserId(authorId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(u), null, List.of()));

        when(commentService.addCommentToTicket(eq(3L), eq(authorId), any(CommentRequestDto.class)))
                .thenReturn(sampleComment(1L, 3L, authorId));

        String body = "{\"content\": \"Please advise\"}";

        mockMvc.perform(post("/api/comments/ticket/{ticketId}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticketId").value(3));

        verify(commentService).addCommentToTicket(eq(3L), eq(authorId), any(CommentRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/comments/ticket/{ticketId} returns 200 with the PagedModel shape")
    void getCommentsForTicket_returns200() throws Exception {
        UUID authorId = UUID.randomUUID();
        Page<CommentResponseDto> page = new PageImpl<>(List.of(sampleComment(1L, 3L, authorId)));
        when(commentService.getCommentsForTicket(eq(3L), any(Pageable.class), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/comments/ticket/{ticketId}", 3L))
                .andExpect(status().isOk())
                // VIA_DTO shape: content array + nested page metadata
                .andExpect(jsonPath("$.content[0].ticketId").value(3))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(commentService).getCommentsForTicket(eq(3L), any(Pageable.class), any());
    }

    @Test
    @DisplayName("GET /api/comments/ticket/{ticketId} returns 404 when the ticket is missing")
    void getCommentsForTicket_notFound_returns404() throws Exception {
        when(commentService.getCommentsForTicket(eq(99L), any(Pageable.class), any()))
                .thenThrow(new NotFoundException("Ticket 99 not found."));

        mockMvc.perform(get("/api/comments/ticket/{ticketId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket 99 not found."));
    }

    @Test
    @DisplayName("GET /api/comments/ticket/{ticketId}/search forwards the keyword and returns the PagedModel shape")
    void searchComments_forwardsKeyword() throws Exception {
        UUID authorId = UUID.randomUUID();
        Page<CommentResponseDto> page = new PageImpl<>(List.of(sampleComment(1L, 3L, authorId)));
        when(commentService.searchComments(eq(3L), eq("advise"), any(Pageable.class), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/comments/ticket/{ticketId}/search", 3L).param("keyword", "advise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(commentService).searchComments(eq(3L), eq("advise"), any(Pageable.class), any());
    }

    @Test
    @DisplayName("GET /api/comments/ticket/{ticketId}/search returns 403 when the service forbids the caller")
    void searchComments_forbidden_returns403() throws Exception {
        when(commentService.searchComments(eq(3L), eq("advise"), any(Pageable.class), any()))
                .thenThrow(new ForbiddenException("You are not allowed to view this ticket"));

        mockMvc.perform(get("/api/comments/ticket/{ticketId}/search", 3L).param("keyword", "advise"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to view this ticket"));
    }

    @Test
    @DisplayName("POST /api/comments/ticket/{ticketId} returns 404 when the author is missing")
    void addComment_authorMissing_returns404() throws Exception {
        UUID authorId = UUID.randomUUID();

        User u = new User();
        u.setUserId(authorId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(u), null, List.of()));

        when(commentService.addCommentToTicket(eq(3L), eq(authorId), any(CommentRequestDto.class)))
                .thenThrow(new NotFoundException("User: " + authorId + " does not exist."));

        String body = "{\"content\": \"Please advise\"}";

        mockMvc.perform(post("/api/comments/ticket/{ticketId}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User: " + authorId + " does not exist."));
    }

    @Test
    @DisplayName("DELETE /api/comments/{commentId} returns 204")
    void deleteComment_returns204() throws Exception {
        mockMvc.perform(delete("/api/comments/{commentId}", 7L))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(7L);
    }

    @Test
    @DisplayName("PUT /api/comments/{commentId} returns 200 and forwards the update")
    void updateComment_returns200() throws Exception {
        UUID authorId = UUID.randomUUID();
        when(commentService.updateComment(eq(7L), any(CommentRequestDto.class), any()))
                .thenReturn(sampleComment(7L, 3L, authorId));

        String body = "{\"content\": \"Updated content\"}";

        mockMvc.perform(put("/api/comments/{commentId}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(commentService).updateComment(eq(7L), any(CommentRequestDto.class), any());
    }
}
