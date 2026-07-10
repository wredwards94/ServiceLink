package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.dtos.TicketStatusUpdateDto;
import com.wesleyedwards.ServiceLink.dtos.TicketUpdateDto;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.TicketMapper;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.TicketService;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    public void deleteTicketById(Long id) {
        Ticket foundTicket = checkTicketExists(id);

        ticketRepository.deleteById(id);
    }

    @Override
    @Transactional
    public TicketResponseDto updateTicket(Long id, TicketUpdateDto updatedTicket) {
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
    public Page<TicketResponseDto> searchTickets(String keyword, Pageable pageable) {
        return ticketRepository.searchByKeyword(keyword, pageable)
                .map(ticketMapper::entityToResponseDto);
    }

    @Override
    public Page<TicketResponseDto> advancedSearch(String keyword, TicketStatus status, TicketPriority priority, Pageable pageable) {
        return ticketRepository.advancedSearch(keyword, status, priority, pageable)
                .map(ticketMapper::entityToResponseDto);
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
        User foundUser = checkUserExists(requesterId);
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByRequester(requesterId));
    }

    @Override
    public List<TicketResponseDto> getTicketsAssignedToUser(UUID userId) {
        User foundUser = checkUserExists(userId);

        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByAssignedToUser(userId));
    }

    @Override
    public TicketResponseDto updateTicketStatus(Long id, TicketStatus status) {
        Ticket foundTicket = checkTicketExists(id);
        if (foundTicket.getStatus().canTransitionTo(status)) throw new BadRequestException("Cannot transition from " + foundTicket.getStatus() + " to " + status);
        foundTicket.setStatus(status);

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
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
