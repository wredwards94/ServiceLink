package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PostMapping("/newticket")
    public ResponseEntity<Ticket> createTicket(@RequestBody Ticket createdTicket) {
        return ResponseEntity.ok(ticketService.createTicket(createdTicket));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Ticket> DeleteTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.deleteTicketById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @RequestBody Ticket updatedTicket) {
        return ResponseEntity.ok(ticketService.updateTicket(id, updatedTicket));
    }
}
