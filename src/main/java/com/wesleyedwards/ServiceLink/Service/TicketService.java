package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Entities.Ticket;

import java.util.List;

public interface TicketService {

    List<Ticket> getAllTickets();
    Ticket createTicket(Ticket createdTicket);

    Ticket getTicketById(Long id);

    Ticket deleteTicketById(Long id);

    Ticket updateTicket(Long id, Ticket updatedTicket);

    List<Ticket> getAllTicketsByStatus(String status);

    List<Ticket> getAllTicketsByPriority(String priority);

    List<Ticket> searchTickets(String keyword);
}
