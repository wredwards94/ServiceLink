package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PostMapping("/newticket/requester{requesterId}")
    public ResponseEntity<Ticket> createTicket(@RequestBody TicketRequestDto createdTicket, @RequestParam UUID requesterId) {
        return ResponseEntity.ok(ticketService.createTicket(createdTicket, requesterId));
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

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Ticket>> getAllTicketByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ticketService.getAllTicketsByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Ticket>> getAllTicketsByPriority(@PathVariable String priority) {
        return ResponseEntity.ok(ticketService.getAllTicketsByPriority(priority));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Ticket>> searchTickets(@RequestParam String keyword) {
        return ResponseEntity.ok(ticketService.searchTickets(keyword));
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<List<Ticket>> advancedSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        List<Ticket> tickets = ticketService.advancedSearch(keyword, status, priority);
        return ResponseEntity.ok(tickets);
    }

    @PutMapping("/{id}/assign/{userId}")
    public ResponseEntity<Ticket> assignTicketToUser(@PathVariable Long id, @PathVariable UUID userId) {
        return ResponseEntity.ok(ticketService.assignTicketToUser(id, userId));
    }

}
