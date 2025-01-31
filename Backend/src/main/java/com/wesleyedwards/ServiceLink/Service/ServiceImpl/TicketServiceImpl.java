package com.wesleyedwards.ServiceLink.Service.ServiceImpl;

import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Entities.User;
import com.wesleyedwards.ServiceLink.Exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.Mappers.TicketMapper;
import com.wesleyedwards.ServiceLink.Repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.Repositories.UserRepository;
import com.wesleyedwards.ServiceLink.Service.TicketService;
import jakarta.transaction.Transactional;
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
    public List<TicketResponseDto> getAllTickets() {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAll());
    }

    @Override
    public TicketResponseDto createTicket(TicketRequestDto createdTicket, UUID requesterID) {
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
        return ticketMapper.entityToResponseDto(newTicket);
    }

    @Override
    public TicketResponseDto getTicketById(Long id) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
//
//        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        return ticketMapper.entityToResponseDto(checkTicketExists(id));
    }

    @Override
    public TicketResponseDto deleteTicketById(Long id) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        Ticket foundTicket = checkTicketExists(id);
//        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        ticketRepository.deleteById(id);
        return ticketMapper.entityToResponseDto(foundTicket);
    }

    @Override
    @Transactional
    public TicketResponseDto updateTicket(Long id, TicketRequestDto updatedTicket) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
//
//        if(optionalTicket.isEmpty()) throw new RuntimeException("Ticket not found");

        Ticket ticket = checkTicketExists(id);

        ticket.setTitle(updatedTicket.title());
        ticket.setDescription(updatedTicket.description());
        ticket.setStatus(updatedTicket.status());
        ticket.setPriority(updatedTicket.priority());
        ticket.setCategory(updatedTicket.category());
//        ticket.setCreatedAt();
//        ticket.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now().withNano(0)));

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public List<TicketResponseDto> getAllTicketsByStatus(String status) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllTicketsByStatus(status));
    }

    @Override
    public List<TicketResponseDto> getAllTicketsByPriority(String priority) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllTicketsByPriority(priority));
    }

    @Override
    public List<TicketResponseDto> searchTickets(String keyword) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.searchByKeyword(keyword));
    }

    @Override
    public List<TicketResponseDto> advancedSearch(String keyword, String status, String priority) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.advancedSearch(keyword, status, priority));
    }

    @Override
    public TicketResponseDto assignTicketToUser(Long id, UUID userId) {
//        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
//        Optional<User> optionalUser = userRepository.findById(userId);

        Ticket foundTicket = checkTicketExists(id);
        User foundUser = checkUserExists(userId);

        foundTicket.setAssignedTo(foundUser);
//        foundTicket.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now().withNano(0)));

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
    }

    @Override
    public List<TicketResponseDto> getTicketsByRequester(UUID requesterId) {
        User foundUser = checkUserExists(requesterId);

        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByRequester(requesterId));
    }

    @Override
    public List<TicketResponseDto> getTicketsAssignedToUser(UUID userId) {
        User foundUser = checkUserExists(userId);

        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByAssignedToUser(userId));
    }

//    @Override
//    public List<CommentResponseDto> getCommentsForTicket(Long id) {
//        checkTicketExists(id);
//
//
//        return ;
//    }

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
