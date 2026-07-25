package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.entities.Ticket;

public interface TicketAccessPolicy {
    void assertCanView(UserPrincipal actor, Ticket ticket);
}
