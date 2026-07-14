package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.BulkAssignDto;
import com.wesleyedwards.ServiceLink.dtos.BulkFailureDto;
import com.wesleyedwards.ServiceLink.dtos.BulkResultDto;
import com.wesleyedwards.ServiceLink.dtos.BulkStatusDto;
import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.dtos.TicketStatusUpdateDto;
import com.wesleyedwards.ServiceLink.dtos.TicketUpdateDto;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.TicketMapper;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImpl")
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks private TicketServiceImpl ticketService;

    private Ticket ticket;
    private TicketResponseDto ticketDto;
    private final Long ticketId = 1L;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTitle("Login broken");
        ticket.setStatus(TicketStatus.NEW);
        ticket.setPriority(TicketPriority.HIGH);

        ticketDto = sampleDto();
    }

    private TicketResponseDto sampleDto() {
        return new TicketResponseDto(ticketId, "Login broken", "desc",
                TicketStatus.NEW, TicketPriority.HIGH, "Technical",
                null, null, null, null, List.of());
    }

    private UserPrincipal principal(UUID id, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        return new UserPrincipal(u);
    }

    @Test
    @DisplayName("getAllTickets returns every ticket for staff")
    void getAllTickets_staffSeesAll() {
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(ticketRepository.findAll()).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getAllTickets(principal(userId, Role.ADMIN)));
    }

    @Test
    @DisplayName("getAllTickets returns only the caller's tickets for a USER")
    void getAllTickets_userSeesOwn() {
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(ticketRepository.findAllByRequester(userId)).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getAllTickets(principal(userId, Role.USER)));
        verify(ticketRepository, never()).findAll();
    }

    @Test
    @DisplayName("createTicket attaches the requester and saves")
    void createTicket_setsRequester() {
        TicketRequestDto request = new TicketRequestDto("Login broken", "desc",
                TicketPriority.HIGH, "Technical");
        User requester = new User();
        requester.setUserId(userId);

        when(ticketMapper.requestDtoToEntity(request)).thenReturn(ticket);
        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        TicketResponseDto result = ticketService.createTicket(request, userId);

        assertSame(ticketDto, result);
        assertSame(requester, ticket.getRequester());
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    @DisplayName("createTicket throws when the requester does not exist")
    void createTicket_requesterMissing() {
        TicketRequestDto request = new TicketRequestDto("Login broken", "desc",
                TicketPriority.HIGH, "Technical");
        when(ticketMapper.requestDtoToEntity(request)).thenReturn(ticket);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.createTicket(request, userId));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("getTicketById returns the ticket for staff")
    void getTicketById_staffSees() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        assertSame(ticketDto, ticketService.getTicketById(ticketId, principal(userId, Role.AGENT)));
    }

    @Test
    @DisplayName("getTicketById returns the ticket for its requester")
    void getTicketById_ownerSees() {
        User requester = new User();
        requester.setUserId(userId);
        ticket.setRequester(requester);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        assertSame(ticketDto, ticketService.getTicketById(ticketId, principal(userId, Role.USER)));
    }

    @Test
    @DisplayName("getTicketById forbids a USER who is not the requester")
    void getTicketById_nonOwnerForbidden() {
        User requester = new User();
        requester.setUserId(UUID.randomUUID()); // someone else
        ticket.setRequester(requester);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(ForbiddenException.class,
                () -> ticketService.getTicketById(ticketId, principal(userId, Role.USER)));
        verify(ticketMapper, never()).entityToResponseDto(any(Ticket.class));
    }

    @Test
    @DisplayName("getTicketById throws NotFoundException when missing")
    void getTicketById_missing() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ticketService.getTicketById(ticketId, principal(userId, Role.ADMIN)));
    }

    @Test
    @DisplayName("deleteTicketById deletes an existing ticket")
    void deleteTicketById_deletes() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicketById(ticketId);

        verify(ticketRepository).deleteById(ticketId);
    }

    @Test
    @DisplayName("deleteTicketById throws and does not delete when missing")
    void deleteTicketById_missing() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.deleteTicketById(ticketId));
        verify(ticketRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("updateTicket applies the changes and saves")
    void updateTicket_updates() {
        TicketUpdateDto update = new TicketUpdateDto("New title", "new desc",
                TicketPriority.LOW, "Billing");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        assertSame(ticketDto, ticketService.updateTicket(ticketId, update));
        verify(ticketMapper).updateTicketFromDto(update, ticket);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    @DisplayName("updateTicketStatus applies an allowed transition and saves")
    void updateTicketStatus_validTransition() {
        // ticket starts NEW; NEW -> IN_PROGRESS is allowed
        TicketStatusUpdateDto dto = new TicketStatusUpdateDto(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        assertSame(ticketDto, ticketService.updateTicketStatus(ticketId, dto));
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    @DisplayName("updateTicketStatus rejects an illegal transition without saving")
    void updateTicketStatus_illegalTransition() {
        // ticket starts NEW; NEW -> CLOSED is not allowed
        TicketStatusUpdateDto dto = new TicketStatusUpdateDto(TicketStatus.CLOSED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(BadRequestException.class,
                () -> ticketService.updateTicketStatus(ticketId, dto));
        assertEquals(TicketStatus.NEW, ticket.getStatus());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("getAllTicketsByStatus is unscoped (null requester) for staff")
    void getAllTicketsByStatus_staffUnscoped() {
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(ticketRepository.findAllTicketsByStatus(TicketStatus.NEW, null)).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getAllTicketsByStatus(TicketStatus.NEW, principal(userId, Role.ADMIN)));
    }

    @Test
    @DisplayName("getAllTicketsByStatus scopes to the caller's own tickets for a USER")
    void getAllTicketsByStatus_userScoped() {
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(ticketRepository.findAllTicketsByStatus(TicketStatus.NEW, userId)).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getAllTicketsByStatus(TicketStatus.NEW, principal(userId, Role.USER)));
    }

    @Test
    @DisplayName("getAllTicketsByPriority is unscoped (null requester) for staff")
    void getAllTicketsByPriority_staffUnscoped() {
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(ticketRepository.findAllTicketsByPriority(TicketPriority.HIGH, null)).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getAllTicketsByPriority(TicketPriority.HIGH, principal(userId, Role.AGENT)));
    }

    @Test
    @DisplayName("getAllTicketsByPriority scopes to the caller's own tickets for a USER")
    void getAllTicketsByPriority_userScoped() {
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(ticketRepository.findAllTicketsByPriority(TicketPriority.HIGH, userId)).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getAllTicketsByPriority(TicketPriority.HIGH, principal(userId, Role.USER)));
    }

    @Test
    @DisplayName("searchTickets maps the page of results, unscoped for staff")
    void searchTickets_mapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(ticket));
        when(ticketRepository.searchByKeyword("login", null, pageable)).thenReturn(page);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        Page<TicketResponseDto> result =
                ticketService.searchTickets("login", pageable, principal(userId, Role.ADMIN));

        assertEquals(1, result.getTotalElements());
        assertSame(ticketDto, result.getContent().get(0));
    }

    @Test
    @DisplayName("searchTickets scopes to the caller's own tickets for a USER")
    void searchTickets_userScoped() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(ticket));
        when(ticketRepository.searchByKeyword("login", userId, pageable)).thenReturn(page);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        Page<TicketResponseDto> result =
                ticketService.searchTickets("login", pageable, principal(userId, Role.USER));

        assertEquals(1, result.getTotalElements());
        verify(ticketRepository).searchByKeyword("login", userId, pageable);
    }

    @Test
    @DisplayName("advancedSearch maps the page of results, unscoped for staff")
    void advancedSearch_mapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(ticket));
        when(ticketRepository.advancedSearch("login", TicketStatus.NEW, TicketPriority.HIGH, null, pageable))
                .thenReturn(page);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        Page<TicketResponseDto> result =
                ticketService.advancedSearch("login", TicketStatus.NEW, TicketPriority.HIGH, pageable,
                        principal(userId, Role.ADMIN));

        assertEquals(1, result.getTotalElements());
        assertSame(ticketDto, result.getContent().get(0));
    }

    @Test
    @DisplayName("advancedSearch scopes to the caller's own tickets for a USER")
    void advancedSearch_userScoped() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(ticket));
        when(ticketRepository.advancedSearch("login", TicketStatus.NEW, TicketPriority.HIGH, userId, pageable))
                .thenReturn(page);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        Page<TicketResponseDto> result =
                ticketService.advancedSearch("login", TicketStatus.NEW, TicketPriority.HIGH, pageable,
                        principal(userId, Role.USER));

        assertEquals(1, result.getTotalElements());
        verify(ticketRepository).advancedSearch("login", TicketStatus.NEW, TicketPriority.HIGH, userId, pageable);
    }

    @Test
    @DisplayName("assignTicketToUser sets the assignee and saves")
    void assignTicketToUser_assigns() {
        User agent = new User();
        agent.setUserId(userId);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
        when(ticketMapper.entityToResponseDto(ticket)).thenReturn(ticketDto);

        assertSame(ticketDto, ticketService.assignTicketToUser(ticketId, userId));
        assertSame(agent, ticket.getAssignedTo());
    }

    @Test
    @DisplayName("assignTicketToUser throws when the assignee does not exist")
    void assignTicketToUser_userMissing() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.assignTicketToUser(ticketId, userId));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("getTicketsByRequester validates the user then returns their tickets")
    void getTicketsByRequester_returnsTickets() {
        User requester = new User();
        requester.setUserId(userId);
        List<Ticket> tickets = List.of(ticket);
        List<TicketResponseDto> dtos = List.of(ticketDto);
        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(ticketRepository.findAllByRequester(userId)).thenReturn(tickets);
        when(ticketMapper.entitiesToResponseDtos(tickets)).thenReturn(dtos);

        assertSame(dtos, ticketService.getTicketsByRequester(userId, principal(userId, Role.USER)));
    }

    @Test
    @DisplayName("getTicketsByRequester forbids a USER querying another user's tickets")
    void getTicketsByRequester_otherUserForbidden() {
        UUID otherId = UUID.randomUUID();

        assertThrows(ForbiddenException.class,
                () -> ticketService.getTicketsByRequester(otherId, principal(userId, Role.USER)));
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getTicketsAssignedToUser throws when the user does not exist")
    void getTicketsAssignedToUser_userMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ticketService.getTicketsAssignedToUser(userId, principal(userId, Role.ADMIN)));
    }

    // ---------- bulk status ----------

    @Test
    @DisplayName("bulkUpdateTicketStatus applies every valid transition")
    void bulkUpdateTicketStatus_allSucceed() {
        // ticket (id 1) is NEW; NEW -> IN_PROGRESS is allowed
        Ticket t2 = new Ticket();
        t2.setId(2L);
        t2.setStatus(TicketStatus.NEW);
        BulkStatusDto dto = new BulkStatusDto(List.of(1L, 2L), TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(t2));

        BulkResultDto result = ticketService.bulkUpdateTicketStatus(dto);

        assertEquals(2, result.succeeded().size());
        assertTrue(result.succeeded().containsAll(List.of(1L, 2L)));
        assertEquals(0, result.failed().size());
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(TicketStatus.IN_PROGRESS, t2.getStatus());
    }

    @Test
    @DisplayName("bulkUpdateTicketStatus keeps going on failures (partial success)")
    void bulkUpdateTicketStatus_partialSuccess() {
        // ticket (id 1) NEW -> IN_PROGRESS ok; ticket 2 CLOSED -> IN_PROGRESS illegal; 99 missing
        Ticket closed = new Ticket();
        closed.setId(2L);
        closed.setStatus(TicketStatus.CLOSED);
        BulkStatusDto dto = new BulkStatusDto(List.of(1L, 2L, 99L), TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(closed));
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        BulkResultDto result = ticketService.bulkUpdateTicketStatus(dto);

        assertEquals(List.of(1L), result.succeeded());
        List<Long> failedIds = result.failed().stream().map(BulkFailureDto::ticketId).toList();
        assertEquals(2, failedIds.size());
        assertTrue(failedIds.containsAll(List.of(2L, 99L)));
        assertEquals(TicketStatus.CLOSED, closed.getStatus()); // unchanged
    }

    // ---------- bulk assign ----------

    @Test
    @DisplayName("bulkAssignTickets assigns every existing ticket to the assignee")
    void bulkAssignTickets_allSucceed() {
        User assignee = new User();
        assignee.setUserId(userId);
        Ticket t2 = new Ticket();
        t2.setId(2L);
        BulkAssignDto dto = new BulkAssignDto(List.of(1L, 2L), userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(t2));

        BulkResultDto result = ticketService.bulkAssignTickets(dto, principal(userId, Role.ADMIN));

        assertEquals(2, result.succeeded().size());
        assertEquals(0, result.failed().size());
        assertSame(assignee, ticket.getAssignedTo());
        assertSame(assignee, t2.getAssignedTo());
    }

    @Test
    @DisplayName("bulkAssignTickets fails the whole request when the assignee is missing")
    void bulkAssignTickets_missingAssignee() {
        BulkAssignDto dto = new BulkAssignDto(List.of(1L, 2L), userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ticketService.bulkAssignTickets(dto, principal(userId, Role.ADMIN)));
        verify(ticketRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("bulkAssignTickets keeps going when a ticket is missing (partial success)")
    void bulkAssignTickets_partialSuccess() {
        User assignee = new User();
        assignee.setUserId(userId);
        BulkAssignDto dto = new BulkAssignDto(List.of(1L, 99L), userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        BulkResultDto result = ticketService.bulkAssignTickets(dto, principal(userId, Role.ADMIN));

        assertEquals(List.of(1L), result.succeeded());
        assertEquals(1, result.failed().size());
        assertEquals(99L, result.failed().get(0).ticketId());
        assertSame(assignee, ticket.getAssignedTo());
    }
}
