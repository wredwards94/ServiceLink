package com.wesleyedwards.ServiceLink.Repositories;

import com.wesleyedwards.ServiceLink.Entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
