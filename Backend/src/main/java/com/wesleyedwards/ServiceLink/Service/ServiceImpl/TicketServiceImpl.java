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
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
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

        ticketRepository.saveAndFlush(newTicket);
        return ticketMapper.entityToResponseDto(newTicket);
    }

    @Override
    public TicketResponseDto getTicketById(Long id) {
        return ticketMapper.entityToResponseDto(checkTicketExists(id));
    }

    @Override
    public TicketResponseDto deleteTicketById(Long id) {
        Ticket foundTicket = checkTicketExists(id);

        ticketRepository.deleteById(id);
        return ticketMapper.entityToResponseDto(foundTicket);
    }

    @Override
    @Transactional
    public TicketResponseDto updateTicket(Long id, TicketRequestDto updatedTicket) {
        Ticket ticket = checkTicketExists(id);
        ticketMapper.updateTicketFromDto(updatedTicket, ticket);
        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    public List<TicketResponseDto> getAllTicketsByStatus(TicketStatus status) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllTicketsByStatus(status));
    }

    @Override
    public List<TicketResponseDto> getAllTicketsByPriority(TicketPriority priority) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllTicketsByPriority(priority));
    }

    @Override
    public List<TicketResponseDto> searchTickets(String keyword) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.searchByKeyword(keyword));
    }

    @Override
    public List<TicketResponseDto> advancedSearch(String keyword, TicketStatus status, TicketPriority priority) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.advancedSearch(keyword, status, priority));
    }

    @Override
    public TicketResponseDto assignTicketToUser(Long id, UUID userId) {
        Ticket foundTicket = checkTicketExists(id);
        User foundUser = checkUserExists(userId);

        foundTicket.setAssignedTo(foundUser);

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
    }

    @Override
    public List<TicketResponseDto> getTicketsByRequester(UUID requesterId) {
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByRequester(requesterId));
    }

    @Override
    public List<TicketResponseDto> getTicketsAssignedToUser(UUID userId) {
        User foundUser = checkUserExists(userId);

        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByAssignedToUser(userId));
    }

    private Ticket checkTicketExists(Long id) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if(optionalTicket.isEmpty()) throw new NotFoundException("Ticket " + id + " not found");

        return optionalTicket.get();
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist");

        return optionalUser.get();
    }
}
