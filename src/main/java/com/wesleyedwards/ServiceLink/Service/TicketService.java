package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Entities.Ticket;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    List<Ticket> getAllTickets();
    Ticket createTicket(Ticket createdTicket, UUID requesterId);

    Ticket getTicketById(Long id);

    Ticket deleteTicketById(Long id);

    Ticket updateTicket(Long id, Ticket updatedTicket);

    List<Ticket> getAllTicketsByStatus(String status);

    List<Ticket> getAllTicketsByPriority(String priority);

    List<Ticket> searchTickets(String keyword);

    List<Ticket> advancedSearch(String keyword, String status, String priority);

    Ticket assignTicketToUser(Long id, UUID userId);
}
