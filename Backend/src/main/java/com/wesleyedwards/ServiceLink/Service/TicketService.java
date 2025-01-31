package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketResponseDto;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    List<TicketResponseDto> getAllTickets();
    TicketResponseDto createTicket(TicketRequestDto createdTicket, UUID requesterId);

    TicketResponseDto getTicketById(Long id);

    TicketResponseDto deleteTicketById(Long id);

    TicketResponseDto updateTicket(Long id, TicketRequestDto updatedTicket);

    List<TicketResponseDto> getAllTicketsByStatus(String status);

    List<TicketResponseDto> getAllTicketsByPriority(String priority);

    List<TicketResponseDto> searchTickets(String keyword);

    List<TicketResponseDto> advancedSearch(String keyword, String status, String priority);

    TicketResponseDto assignTicketToUser(Long id, UUID userId);

    List<TicketResponseDto> getTicketsByRequester(UUID requesterId);

    List<TicketResponseDto> getTicketsAssignedToUser(UUID userId);

//    List<CommentResponseDto> getCommentsForTicket(Long id);
}
