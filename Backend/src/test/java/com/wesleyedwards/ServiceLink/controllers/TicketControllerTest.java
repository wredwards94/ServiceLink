package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.JwtAuthFilter;
import com.wesleyedwards.ServiceLink.config.SecurityConfig;
import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.dtos.TicketUpdateDto;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.service.TicketService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TicketController")
class TicketControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TicketService ticketService;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private TicketResponseDto sampleTicket(Long id) {
        return new TicketResponseDto(id, "Login broken", "desc",
                TicketStatus.NEW, TicketPriority.HIGH, "Technical",
                null, null, null, null, List.of());
    }

    @Test
    @DisplayName("GET /api/tickets returns 200 with the ticket list")
    void getAllTickets_returns200() throws Exception {
        when(ticketService.getAllTickets(any())).thenReturn(List.of(sampleTicket(1L)));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("POST /api/tickets/newticket/requester returns 201 using the authenticated user")
    void createTicket_returns201() throws Exception {
        UUID requesterId = UUID.randomUUID();

        User u = new User();
        u.setUserId(requesterId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(u), null, List.of()));

        when(ticketService.createTicket(any(TicketRequestDto.class), eq(requesterId)))
                .thenReturn(sampleTicket(1L));

        String body = """
                {
                  "title": "Login broken",
                  "description": "desc",
                  "status": "NEW",
                  "priority": "HIGH",
                  "category": "Technical"
                }
                """;

        mockMvc.perform(post("/api/tickets/newticket/requester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(ticketService).createTicket(any(TicketRequestDto.class), eq(requesterId));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} returns 200")
    void getTicketById_returns200() throws Exception {
        when(ticketService.getTicketById(eq(5L), any())).thenReturn(sampleTicket(5L));

        mockMvc.perform(get("/api/tickets/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(ticketService).getTicketById(eq(5L), any());
    }

    @Test
    @DisplayName("GET /api/tickets/{id} returns 404 when the ticket is missing")
    void getTicketById_notFound_returns404() throws Exception {
        when(ticketService.getTicketById(eq(99L), any()))
                .thenThrow(new NotFoundException("Ticket 99 not found"));

        mockMvc.perform(get("/api/tickets/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket 99 not found"));
    }

    @Test
    @DisplayName("DELETE /api/tickets/{id} returns 204")
    void deleteTicket_returns204() throws Exception {
        mockMvc.perform(delete("/api/tickets/{id}", 5L))
                .andExpect(status().isNoContent());

        verify(ticketService).deleteTicketById(5L);
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id} returns 200 and forwards the update")
    void updateTicket_returns200() throws Exception {
        when(ticketService.updateTicket(eq(5L), any(TicketUpdateDto.class))).thenReturn(sampleTicket(5L));

        String body = """
                {
                  "title": "Updated",
                  "description": "desc",
                  "status": "IN_PROGRESS",
                  "priority": "LOW",
                  "category": "Billing"
                }
                """;

        mockMvc.perform(patch("/api/tickets/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(ticketService).updateTicket(eq(5L), any(TicketUpdateDto.class));
    }

    @Test
    @DisplayName("GET /api/tickets/status/{status} binds the enum path variable")
    void getByStatus_bindsEnum() throws Exception {
        when(ticketService.getAllTicketsByStatus(TicketStatus.NEW))
                .thenReturn(List.of(sampleTicket(1L)));

        mockMvc.perform(get("/api/tickets/status/{status}", "NEW"))
                .andExpect(status().isOk());

        verify(ticketService).getAllTicketsByStatus(TicketStatus.NEW);
    }

    @Test
    @DisplayName("GET /api/tickets/priority/{priority} binds the enum path variable")
    void getByPriority_bindsEnum() throws Exception {
        when(ticketService.getAllTicketsByPriority(TicketPriority.HIGH))
                .thenReturn(List.of(sampleTicket(1L)));

        mockMvc.perform(get("/api/tickets/priority/{priority}", "HIGH"))
                .andExpect(status().isOk());

        verify(ticketService).getAllTicketsByPriority(TicketPriority.HIGH);
    }

    @Test
    @DisplayName("GET /api/tickets/search forwards keyword and a pageable")
    void searchTickets_forwardsKeyword() throws Exception {
        Page<TicketResponseDto> page = new PageImpl<>(List.of(sampleTicket(1L)));
        when(ticketService.searchTickets(eq("login"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/tickets/search").param("keyword", "login"))
                .andExpect(status().isOk());

        verify(ticketService).searchTickets(eq("login"), any(Pageable.class));
    }

    @Test
    @DisplayName("PUT /api/tickets/{id}/assign/{userId} returns 200 and binds both path variables")
    void assignTicket_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(ticketService.assignTicketToUser(eq(5L), eq(userId))).thenReturn(sampleTicket(5L));

        mockMvc.perform(put("/api/tickets/{id}/assign/{userId}", 5L, userId))
                .andExpect(status().isOk());

        verify(ticketService).assignTicketToUser(5L, userId);
    }

    @Test
    @DisplayName("PUT /api/tickets/{id}/assign/{userId} returns 404 when the assignee is missing")
    void assignTicket_userMissing_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(ticketService.assignTicketToUser(eq(5L), eq(userId)))
                .thenThrow(new NotFoundException("User: " + userId + " does not exist"));

        mockMvc.perform(put("/api/tickets/{id}/assign/{userId}", 5L, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User: " + userId + " does not exist"));
    }

    @Test
    @DisplayName("GET /api/tickets/requester/{requesterId} returns 200")
    void getByRequester_returns200() throws Exception {
        UUID requesterId = UUID.randomUUID();
        when(ticketService.getTicketsByRequester(eq(requesterId), any())).thenReturn(List.of(sampleTicket(1L)));

        mockMvc.perform(get("/api/tickets/requester/{requesterId}", requesterId))
                .andExpect(status().isOk());

        verify(ticketService).getTicketsByRequester(eq(requesterId), any());
    }
}
