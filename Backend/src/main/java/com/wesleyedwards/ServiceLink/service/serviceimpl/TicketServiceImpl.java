package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.ServiceLinkRevision;
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
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;
    private final EntityManager em;

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

    @Override
    @Transactional
    public List<TicketHistoryEntryDto> getTicketHistory(Long id, UserPrincipal actor) {
        Ticket ticket = checkTicketExists(id);
        assertCanView(actor, ticket);

        AuditReader reader = AuditReaderFactory.get(em);

        // Each row is Object[]{ Ticket snapshot, ServiceLinkRevision, RevisionType }.
        // Ordered oldest-first so each snapshot can be diffed against the previous one.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Ticket.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        List<TicketHistoryEntryDto> history = new ArrayList<>();
        Ticket previous = null;

        for (Object[] row : rows) {
            Ticket snapshot = (Ticket) row[0];
            ServiceLinkRevision revision = (ServiceLinkRevision) row[1];
            RevisionType type = (RevisionType) row[2];

            int revNumber = revision.getId();
            LocalDateTime when = LocalDateTime.ofInstant(
                    revision.getRevisionDate().toInstant(), ZoneId.systemDefault());
            String actorName = revision.getActorName();
            UUID actorId = revision.getActorId();

            switch (type) {
                case ADD -> history.add(new TicketHistoryEntryDto(
                        revNumber, when, actorName, actorId, "CREATED", null, null, null));
                case DEL -> history.add(new TicketHistoryEntryDto(
                        revNumber, when, actorName, actorId, "DELETED", null, null, null));
                // NOTE: @SoftDelete deletes surface as MOD (not DEL) and the soft-delete
                // flag is not an audited property, so a delete produces no field changes
                // below. Verify empirically and special-case it if a DELETED row is wanted.
                case MOD -> addFieldChanges(
                        history, previous, snapshot, revNumber, when, actorName, actorId);
            }
            previous = snapshot;
        }
        return history;
    }

    // Emit one MODIFIED entry per audited field that differs between the previous and
    // current snapshot. prev is null when the first recorded revision is a MOD (e.g.
    // Envers enabled after the row already existed) — every field then reads as null -> value.
    private void addFieldChanges(List<TicketHistoryEntryDto> history, Ticket prev, Ticket curr,
                                 int rev, LocalDateTime when, String actorName, UUID actorId) {
        addChange(history, rev, when, actorName, actorId, "title",
                prev == null ? null : prev.getTitle(), curr.getTitle());
        addChange(history, rev, when, actorName, actorId, "description",
                prev == null ? null : prev.getDescription(), curr.getDescription());
        addChange(history, rev, when, actorName, actorId, "status",
                enumName(prev == null ? null : prev.getStatus()), enumName(curr.getStatus()));
        addChange(history, rev, when, actorName, actorId, "priority",
                enumName(prev == null ? null : prev.getPriority()), enumName(curr.getPriority()));
        addChange(history, rev, when, actorName, actorId, "category",
                prev == null ? null : prev.getCategory(), curr.getCategory());
        addChange(history, rev, when, actorName, actorId, "assignedTo",
                username(prev == null ? null : prev.getAssignedTo()), username(curr.getAssignedTo()));
    }

    private void addChange(List<TicketHistoryEntryDto> history, int rev, LocalDateTime when,
                           String actorName, UUID actorId, String field, String oldVal, String newVal) {
        if (!Objects.equals(oldVal, newVal)) {
            history.add(new TicketHistoryEntryDto(
                    rev, when, actorName, actorId, "MODIFIED", field, oldVal, newVal));
        }
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String username(User user) {
        if (user == null) return null;
        return user.getCredentials() != null
                ? user.getCredentials().getUsername()
                : String.valueOf(user.getUserId());
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
