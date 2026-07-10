package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.dtos.TicketStatusUpdateDto;
import com.wesleyedwards.ServiceLink.dtos.TicketUpdateDto;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.TicketMapper;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.TicketService;
import com.wesleyedwards.ServiceLink.enums.Role;
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
    public List<TicketResponseDto> getAllTickets(UserPrincipal actor) {
        // Staff (ADMIN/AGENT) see every ticket; a plain USER sees only their own.
        List<Ticket> tickets = isStaff(actor)
                ? ticketRepository.findAll()
                : ticketRepository.findAllByRequester(actor.getUserId());
        return ticketMapper.entitiesToResponseDtos(tickets);
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
    public TicketResponseDto getTicketById(Long id, UserPrincipal actor) {
        Ticket ticket = checkTicketExists(id);
        assertCanView(actor, ticket);
        return ticketMapper.entityToResponseDto(ticket);
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
    public List<TicketResponseDto> getTicketsByRequester(UUID requesterId, UserPrincipal actor) {
        // A USER may only query their own tickets; staff may query anyone's.
        assertSelfOrStaff(actor, requesterId);
        checkUserExists(requesterId);
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByRequester(requesterId));
    }

    @Override
    public List<TicketResponseDto> getTicketsAssignedToUser(UUID userId, UserPrincipal actor) {
        assertSelfOrStaff(actor, userId);
        checkUserExists(userId);
        return ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByAssignedToUser(userId));
    }

    @Override
    public TicketResponseDto updateTicketStatus(Long id, TicketStatusUpdateDto status) {
        Ticket foundTicket = checkTicketExists(id);
        if (!foundTicket.getStatus().canTransitionTo(status.ticketStatus())) throw new BadRequestException("Cannot " +
                "transition from " + foundTicket.getStatus() + " to " + status.ticketStatus());
        foundTicket.setStatus(status.ticketStatus());

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
    }

    private boolean isStaff(UserPrincipal actor) {
        return actor.getRole() == Role.ADMIN || actor.getRole() == Role.AGENT;
    }

    // A USER may only view a ticket they requested; staff may view any ticket.
    private void assertCanView(UserPrincipal actor, Ticket ticket) {
        if (isStaff(actor)) return;
        boolean isRequester = ticket.getRequester() != null
                && ticket.getRequester().getUserId().equals(actor.getUserId());
        if (!isRequester) {
            throw new ForbiddenException("You are not allowed to view this ticket");
        }
    }

    // A USER may only act on their own id; staff may act on anyone's.
    private void assertSelfOrStaff(UserPrincipal actor, UUID targetUserId) {
        if (!isStaff(actor) && !targetUserId.equals(actor.getUserId())) {
            throw new ForbiddenException("You are not allowed to view these tickets");
        }
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
