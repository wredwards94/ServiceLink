package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    List<TicketResponseDto> getAllTickets();
    TicketResponseDto createTicket(TicketRequestDto createdTicket, UUID requesterId);

    TicketResponseDto getTicketById(Long id);

    void deleteTicketById(Long id);

    TicketResponseDto updateTicket(Long id, TicketRequestDto updatedTicket);

    List<TicketResponseDto> getAllTicketsByStatus(TicketStatus status);

    List<TicketResponseDto> getAllTicketsByPriority(TicketPriority priority);

    Page<TicketResponseDto> searchTickets(String keyword, Pageable pageable);

    Page<TicketResponseDto> advancedSearch(String keyword, TicketStatus status, TicketPriority priority, Pageable pageable);

    TicketResponseDto assignTicketToUser(Long id, UUID userId);

    List<TicketResponseDto> getTicketsByRequester(UUID requesterId);

    List<TicketResponseDto> getTicketsAssignedToUser(UUID userId);

//    List<CommentResponseDto> getCommentsForTicket(Long id);
}
