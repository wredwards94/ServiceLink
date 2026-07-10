package com.wesleyedwards.ServiceLink.enums;

public enum TicketStatus {
    NEW, IN_PROGRESS, ON_HOLD, RESOLVED, REOPENED, CLOSED;

    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case NEW        -> target == IN_PROGRESS;
            case IN_PROGRESS-> target == ON_HOLD || target == RESOLVED;
            case ON_HOLD    -> target == IN_PROGRESS;
            case RESOLVED   -> target == CLOSED || target == REOPENED;
            case CLOSED     -> target == REOPENED;
            case REOPENED   -> target == IN_PROGRESS;
        };
    }
}
