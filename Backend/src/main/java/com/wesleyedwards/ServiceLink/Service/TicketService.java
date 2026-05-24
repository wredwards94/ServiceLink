package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    List<TicketResponseDto> getAllTickets();
    TicketResponseDto createTicket(TicketRequestDto createdTicket, UUID requesterId);

    TicketResponseDto getTicketById(Long id);

    TicketResponseDto deleteTicketById(Long id);

    TicketResponseDto updateTicket(Long id, TicketRequestDto updatedTicket);

    List<TicketResponseDto> getAllTicketsByStatus(TicketStatus status);

    List<TicketResponseDto> getAllTicketsByPriority(TicketPriority priority);

    List<TicketResponseDto> searchTickets(String keyword);

    List<TicketResponseDto> advancedSearch(String keyword, TicketStatus status, TicketPriority priority);

    TicketResponseDto assignTicketToUser(Long id, UUID userId);

    List<TicketResponseDto> getTicketsByRequester(UUID requesterId);

    List<TicketResponseDto> getTicketsAssignedToUser(UUID userId);

//    List<CommentResponseDto> getCommentsForTicket(Long id);
}
