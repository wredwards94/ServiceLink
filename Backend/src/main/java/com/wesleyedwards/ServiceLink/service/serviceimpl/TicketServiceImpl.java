package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.*;
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

import java.util.ArrayList;
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
        return hideInternalComments(ticketMapper.entitiesToResponseDtos(tickets), actor);
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
        return hideInternalComments(ticketMapper.entityToResponseDto(ticket), actor);
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
    public List<TicketResponseDto> getAllTicketsByStatus(TicketStatus status, UserPrincipal actor) {
        return hideInternalComments(ticketMapper.entitiesToResponseDtos(
                ticketRepository.findAllTicketsByStatus(status, requesterScope(actor))), actor);
    }

    @Override
    public List<TicketResponseDto> getAllTicketsByPriority(TicketPriority priority, UserPrincipal actor) {
        return hideInternalComments(ticketMapper.entitiesToResponseDtos(
                ticketRepository.findAllTicketsByPriority(priority, requesterScope(actor))), actor);
    }

    @Override
    public Page<TicketResponseDto> searchTickets(String keyword, Pageable pageable, UserPrincipal actor) {
        return ticketRepository.searchByKeyword(keyword, requesterScope(actor), pageable)
                .map(ticket -> hideInternalComments(ticketMapper.entityToResponseDto(ticket), actor));
    }

    @Override
    public Page<TicketResponseDto> advancedSearch(String keyword, TicketStatus status, TicketPriority priority, Pageable pageable, UserPrincipal actor) {
        return ticketRepository.advancedSearch(keyword, status, priority, requesterScope(actor), pageable)
                .map(ticket -> hideInternalComments(ticketMapper.entityToResponseDto(ticket), actor));
    }


    @Override
    public TicketResponseDto assignTicketToUser(Long id, UUID userId) {
        Ticket foundTicket = checkTicketExists(id);
        User foundUser = checkUserExists(userId);

        foundTicket.setAssignedTo(foundUser);

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
    }

    @Override
    public TicketResponseDto unassignTicket(Long id) {
        Ticket foundTicket = checkTicketExists(id);
        foundTicket.setAssignedTo(null);
        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
    }

    @Override
    public List<TicketResponseDto> getTicketsByRequester(UUID requesterId, UserPrincipal actor) {
        // A USER may only query their own tickets; staff may query anyone's.
        assertSelfOrStaff(actor, requesterId);
        checkUserExists(requesterId);
        return hideInternalComments(
                ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByRequester(requesterId)), actor);
    }

    @Override
    public List<TicketResponseDto> getTicketsAssignedToUser(UUID userId, UserPrincipal actor) {
        checkUserExists(userId);
        assertSelfOrStaff(actor, userId);
        return hideInternalComments(
                ticketMapper.entitiesToResponseDtos(ticketRepository.findAllByAssignedToUser(userId)), actor);
    }

    @Override
    public TicketResponseDto updateTicketStatus(Long id, TicketStatusUpdateDto status) {
        Ticket foundTicket = checkTicketExists(id);
        if (!foundTicket.getStatus().canTransitionTo(status.ticketStatus())) throw new BadRequestException("Cannot " +
                "transition from " + foundTicket.getStatus() + " to " + status.ticketStatus());
        foundTicket.setStatus(status.ticketStatus());

        return ticketMapper.entityToResponseDto(ticketRepository.saveAndFlush(foundTicket));
    }

    // NOTE: intentionally NOT @Transactional. Partial success requires each ticket's
    // saveAndFlush to auto-commit independently; a wrapping transaction would roll back
    // the successful items when a later item throws.
    @Override
    public BulkResultDto bulkAssignTickets(BulkAssignDto bulkAssignDto, UserPrincipal actor) {
        // Batch-level: the assignee is the same for every ticket, so validate once.
        // A missing assignee fails the whole request (404) rather than every item.
        User assignee = checkUserExists(bulkAssignDto.userId());

        List<Long> succeeded = new ArrayList<>();
        List<BulkFailureDto> failed = new ArrayList<>();

        for (Long ticketId : bulkAssignDto.ticketIds()) {
            try {
                Ticket ticket = checkTicketExists(ticketId);
                ticket.setAssignedTo(assignee);
                ticketRepository.saveAndFlush(ticket);
                succeeded.add(ticketId);
            } catch (NotFoundException e) {
                failed.add(new BulkFailureDto(ticketId, e.getMessage()));
            }
        }
        return new BulkResultDto(succeeded, failed);
    }

    // NOTE: intentionally NOT @Transactional — see bulkAssignTickets.
    @Override
    public BulkResultDto bulkUpdateTicketStatus(BulkStatusDto bulkStatusDto) {
        List<Long> succeeded = new ArrayList<>();
        List<BulkFailureDto> failed = new ArrayList<>();

        for (Long ticketId : bulkStatusDto.ticketIds()) {
            try {
                Ticket ticket = checkTicketExists(ticketId);
                if (!ticket.getStatus().canTransitionTo(bulkStatusDto.status())) {
                    throw new BadRequestException("Cannot transition from " + ticket.getStatus()
                            + " to " + bulkStatusDto.status());
                }
                ticket.setStatus(bulkStatusDto.status());
                ticketRepository.saveAndFlush(ticket);
                succeeded.add(ticketId);
            } catch (NotFoundException | BadRequestException e) {
                failed.add(new BulkFailureDto(ticketId, e.getMessage()));
            }
        }
        return new BulkResultDto(succeeded, failed);
    }

    private boolean isStaff(UserPrincipal actor) {
        return actor.getRole() == Role.ADMIN || actor.getRole() == Role.AGENT;
    }

    // Internal comments are staff-only. The ticket mapper embeds every comment, so for a
    // non-staff viewer strip internal ones out of the embedded list before returning.
    // (Object identity is preserved when there is nothing to remove, so staff results and
    // internal-free tickets are returned unchanged.)
    private TicketResponseDto hideInternalComments(TicketResponseDto dto, UserPrincipal actor) {
        if (actor.isStaff() || dto.comments() == null
                || dto.comments().stream().noneMatch(CommentResponseDto::internal)) {
            return dto;
        }
        List<CommentResponseDto> visible = dto.comments().stream()
                .filter(c -> !c.internal())
                .toList();
        return new TicketResponseDto(dto.id(), dto.title(), dto.description(), dto.status(),
                dto.priority(), dto.category(), dto.assignedTo(), dto.requester(),
                dto.createdAt(), dto.updatedAt(), visible);
    }

    private List<TicketResponseDto> hideInternalComments(List<TicketResponseDto> dtos, UserPrincipal actor) {
        if (actor.isStaff()) return dtos;
        return dtos.stream().map(dto -> hideInternalComments(dto, actor)).toList();
    }

    // Repository scope for requester-filtered queries: null for staff (unscoped),
    // the actor's own id for a plain USER.
    private UUID requesterScope(UserPrincipal actor) {
        return isStaff(actor) ? null : actor.getUserId();
    }

    // A USER may only view a ticket they requested; staff may view any ticket.
    private void assertCanView(UserPrincipal actor, Ticket ticket) {
        if (actor.isStaff()) return;
        boolean isRequester = ticket.getRequester() != null
                && ticket.getRequester().getUserId().equals(actor.getUserId());
        if (!isRequester) {
            throw new ForbiddenException("You are not allowed to view this ticket");
        }
    }

    // A USER may only act on their own id; staff may act on anyone's.
    private void assertSelfOrStaff(UserPrincipal actor, UUID targetUserId) {
        if (!actor.isStaff() && !targetUserId.equals(actor.getUserId())) {
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
