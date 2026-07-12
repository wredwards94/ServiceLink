package com.wesleyedwards.ServiceLink.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TicketStatus transitions")
class TicketStatusTest {

    @Test
    @DisplayName("allowed transitions return true")
    void allowedTransitions() {
        assertTrue(TicketStatus.NEW.canTransitionTo(TicketStatus.IN_PROGRESS));
        assertTrue(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.ON_HOLD));
        assertTrue(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.RESOLVED));
        assertTrue(TicketStatus.ON_HOLD.canTransitionTo(TicketStatus.IN_PROGRESS));
        assertTrue(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.CLOSED));
        assertTrue(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.REOPENED));
        assertTrue(TicketStatus.CLOSED.canTransitionTo(TicketStatus.REOPENED));
        assertTrue(TicketStatus.REOPENED.canTransitionTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("illegal transitions return false")
    void illegalTransitions() {
        assertFalse(TicketStatus.NEW.canTransitionTo(TicketStatus.CLOSED));
        assertFalse(TicketStatus.NEW.canTransitionTo(TicketStatus.RESOLVED));
        assertFalse(TicketStatus.CLOSED.canTransitionTo(TicketStatus.IN_PROGRESS));
        assertFalse(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("a status cannot transition to itself")
    void noSelfTransition() {
        assertFalse(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.IN_PROGRESS));
        assertFalse(TicketStatus.NEW.canTransitionTo(TicketStatus.NEW));
    }
}
