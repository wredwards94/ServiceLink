package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.AbstractPostgresIT;
import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.TicketHistoryEntryDto;
import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.dtos.TicketStatusUpdateDto;
import com.wesleyedwards.ServiceLink.entities.Credentials;
import com.wesleyedwards.ServiceLink.entities.Profile;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end audit-trail test against a real Postgres (Testcontainers): drives a
 * ticket through create -> status change -> assign -> unassign -> delete and
 * asserts the Envers-derived timeline, actor stamping, ordering, and the
 * soft-delete DELETED event. Boots the full context, so it exercises the real
 * {@code ServiceLinkRevisionListener} and the {@code getTicketHistory} diff logic.
 */
@DisplayName("Ticket history (Envers integration)")
@EnabledIf("com.wesleyedwards.ServiceLink.AbstractPostgresIT#dockerAvailable")
class TicketHistoryTest extends AbstractPostgresIT {

    @Autowired private TicketService ticketService;
    @Autowired private UserRepository userRepository;
    @Autowired private TicketRepository ticketRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("captures the full lifecycle with actor, ordering, and a DELETED event")
    void capturesTicketLifecycle() {
        User requester = saveUser("jsmith", "requester@example.com", Role.USER);
        User agent = saveUser("agent_kate", "kate@example.com", Role.AGENT);

        // Every mutation runs as the agent, so the RevisionListener stamps the agent
        // as the actor on each revision.
        authenticateAs(agent);

        TicketResponseDto created = ticketService.createTicket(
                new TicketRequestDto("Laptop won't boot", "Black screen on power-on",
                        TicketPriority.HIGH, "Hardware"),
                requester.getUserId());
        Long ticketId = created.id();

        ticketService.updateTicketStatus(ticketId, new TicketStatusUpdateDto(TicketStatus.IN_PROGRESS));
        ticketService.assignTicketToUser(ticketId, agent.getUserId());
        ticketService.unassignTicket(ticketId);

        UserPrincipal agentPrincipal = new UserPrincipal(agent);
        List<TicketHistoryEntryDto> history = ticketService.getTicketHistory(ticketId, agentPrincipal);

        assertFalse(history.isEmpty(), "history should not be empty");
        assertEquals("CREATED", history.get(0).type(), "first event should be CREATED");

        assertTrue(hasChange(history, "status", "NEW", "IN_PROGRESS"),
                "expected a MODIFIED status NEW -> IN_PROGRESS event");
        assertTrue(hasChange(history, "assignedTo", null, "agent_kate"),
                "expected a MODIFIED assignedTo null -> agent_kate event");
        assertTrue(hasChange(history, "assignedTo", "agent_kate", null),
                "expected a MODIFIED assignedTo agent_kate -> null event");

        // Actor stamped on every revision, revisions strictly increasing (oldest-first).
        int previousRevision = Integer.MIN_VALUE;
        for (TicketHistoryEntryDto entry : history) {
            assertEquals("agent_kate", entry.actorName(),
                    "every event should be attributed to the acting agent");
            assertNotNull(entry.actorId(), "actorId should be captured");
            assertTrue(entry.revision() > previousRevision, "revisions should be strictly increasing");
            previousRevision = entry.revision();
        }

        // Soft delete: history must stay viewable (not 404) and end with a DELETED event.
        ticketService.deleteTicketById(ticketId);
        List<TicketHistoryEntryDto> afterDelete = ticketService.getTicketHistory(ticketId, agentPrincipal);

        TicketHistoryEntryDto last = afterDelete.get(afterDelete.size() - 1);
        assertEquals("DELETED", last.type(), "deleted ticket history should end with a DELETED event");
        assertEquals("agent_kate", last.actorName(), "the DELETED event should carry the acting agent");
    }

    private User saveUser(String username, String email, Role role) {
        User user = new User();
        user.setCredentials(new Credentials(username, "password"));
        user.setProfile(new Profile("First", "Last", email));
        user.setRole(role);
        return userRepository.save(user);
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private boolean hasChange(List<TicketHistoryEntryDto> history, String field,
                              String oldValue, String newValue) {
        return history.stream().anyMatch(e ->
                "MODIFIED".equals(e.type())
                        && field.equals(e.field())
                        && java.util.Objects.equals(oldValue, e.oldValue())
                        && java.util.Objects.equals(newValue, e.newValue()));
    }
}
