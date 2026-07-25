package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.exceptions.ForbiddenException;
import com.wesleyedwards.ServiceLink.service.TicketAccessPolicy;
import org.springframework.stereotype.Service;

@Service
public class TicketAccessPolicyImpl implements TicketAccessPolicy {
    @Override
    public void assertCanView(UserPrincipal actor, Ticket ticket) {
        if (actor.isStaff()) return;

        boolean isRequester =
                ticket.getRequester() != null && ticket.getRequester().getUserId().equals(actor.getUserId());
        if (!isRequester) {
            throw new ForbiddenException("You are not allowed to view this ticket");
        }
    }
}
