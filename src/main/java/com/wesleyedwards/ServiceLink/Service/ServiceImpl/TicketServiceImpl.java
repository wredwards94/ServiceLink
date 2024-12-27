package com.wesleyedwards.ServiceLink.Service.ServiceImpl;

import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Entities.User;
import com.wesleyedwards.ServiceLink.Exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.Mappers.TicketMapper;
import com.wesleyedwards.ServiceLink.Repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.Repositories.UserRepository;
import com.wesleyedwards.ServiceLink.Service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Override
    public Ticket createTicket(TicketRequestDto createdTicket, UUID requesterID) {
        Ticket newTicket = ticketMapper.requestDtoToEntity(createdTicket);
        User foundUser = checkUserExists(requesterID);
        newTicket.setRequester(foundUser);
//
//        newTicket.setTitle(createdTicket.getTitle());
//        newTicket.setDescription(createdTicket.getDescription());
//        newTicket.setStatus(createdTicket.getStatus());
//        newTicket.setPriority(createdTicket.getPriority());
//        newTicket.setCategory(createdTicket.getCategory());
//        newTicket.setRequester(foundUser);
//        newTicket.setCreatedAt();
//        newTicket.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now().withNano(0)));

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
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        Ticket foundTicket = checkTicketExists(id);
//        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        ticketRepository.deleteById(id);
        return foundTicket;
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
//        ticket.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now().withNano(0)));

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

    @Override
    public List<Ticket> searchTickets(String keyword) {
        return ticketRepository.searchByKeyword(keyword);
    }

    @Override
    public List<Ticket> advancedSearch(String keyword, String status, String priority) {
        return ticketRepository.advancedSearch(keyword, status, priority);
    }

    @Override
    public Ticket assignTicketToUser(Long id, UUID userId) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
//        Optional<User> optionalUser = userRepository.findById(userId);

        Ticket foundTicket = checkTicketExists(id);
        User foundUser = checkUserExists(userId);

        foundTicket.setAssignedTo(foundUser);
//        foundTicket.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now().withNano(0)));

        return ticketRepository.saveAndFlush(foundTicket);
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new NotFoundException("Ticket " + id + " not found");

//        System.out.println(optionalTicket.get().getAssignedTo().getUserId());

        return optionalTicket.get();
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist");

        return optionalUser.get();
    }
}
