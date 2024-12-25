package com.wesleyedwards.ServiceLink.Service.ServiceImpl;

import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.Service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;


    @Override
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Override
    public Ticket createTicket(Ticket createdTicket) {
        Ticket newTicket = new Ticket();

        newTicket.setTitle(createdTicket.getTitle());
        newTicket.setDescription(createdTicket.getDescription());
        newTicket.setStatus(createdTicket.getStatus());
        newTicket.setPriority(createdTicket.getPriority());
        newTicket.setCategory(createdTicket.getCategory());
//        newTicket.setCreatedAt();
        newTicket.setUpdatedAt(newTicket.getCreatedAt());

        ticketRepository.saveAndFlush(newTicket);
        return newTicket;
    }

    @Override
    public Ticket getTicketById(Long id) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
//
//        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        return checkTicketExists(id);
    }

    @Override
    public Ticket deleteTicketById(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        ticketRepository.deleteById(id);
        return optionalTicket.get();
    }

    @Override
    public Ticket updateTicket(Long id, Ticket updatedTicket) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
//
//        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        Ticket ticket = checkTicketExists(id);

        ticket.setTitle(updatedTicket.getTitle());
        ticket.setDescription(updatedTicket.getDescription());
        ticket.setStatus(updatedTicket.getStatus());
        ticket.setPriority(updatedTicket.getPriority());
        ticket.setCategory(updatedTicket.getCategory());
//        ticket.setCreatedAt();
        ticket.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now().withNano(0)));

        return ticketRepository.saveAndFlush(ticket);
    }

    @Override
    public List<Ticket> getAllTicketsByStatus(String status) {
        return ticketRepository.findAllTicketsByStatus(status);
    }

    @Override
    public List<Ticket> getAllTicketsByPriority(String priority) {
        return ticketRepository.findAllTicketsByPriority(priority);
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        return optionalTicket.get();
    }
}
