package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.*;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    List<TicketResponseDto> getAllTickets(UserPrincipal actor);
    TicketResponseDto createTicket(TicketRequestDto createdTicket, UUID requesterId);

    TicketResponseDto getTicketById(Long id, UserPrincipal actor);

    void deleteTicketById(Long id);

    TicketResponseDto updateTicket(Long id, TicketUpdateDto updatedTicket);

    List<TicketResponseDto> getAllTicketsByStatus(TicketStatus status, UserPrincipal actor);

    List<TicketResponseDto> getAllTicketsByPriority(TicketPriority priority, UserPrincipal actor);

    Page<TicketResponseDto> searchTickets(String keyword, Pageable pageable, UserPrincipal actor);

    Page<TicketResponseDto> advancedSearch(String keyword, TicketStatus status, TicketPriority priority, Pageable pageable, UserPrincipal actor);

    TicketResponseDto assignTicketToUser(Long id, UUID userId);

    List<TicketResponseDto> getTicketsByRequester(UUID requesterId, UserPrincipal actor);

    List<TicketResponseDto> getTicketsAssignedToUser(UUID userId, UserPrincipal actor);

    TicketResponseDto updateTicketStatus(Long id, TicketStatusUpdateDto status);

    BulkResultDto bulkAssignTickets(@Valid BulkAssignDto bulkAssignDto, UserPrincipal actor);

    BulkResultDto bulkUpdateTicketStatus(@Valid BulkStatusDto bulkStatusDto);

}
