package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Ticket;
import com.wesleyedwards.ServiceLink.Service.CommentService;
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
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<TicketResponseDto>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PostMapping("/newticket/requester")
    public ResponseEntity<TicketResponseDto> createTicket(@RequestBody TicketRequestDto createdTicket,
                                                          @RequestParam UUID requesterId) {
        return ResponseEntity.ok(ticketService.createTicket(createdTicket, requesterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDto> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TicketResponseDto> DeleteTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.deleteTicketById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponseDto> updateTicket(@PathVariable Long id, @RequestBody TicketRequestDto updatedTicket) {
        return ResponseEntity.ok(ticketService.updateTicket(id, updatedTicket));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketResponseDto>> getAllTicketByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ticketService.getAllTicketsByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TicketResponseDto>> getAllTicketsByPriority(@PathVariable String priority) {
        return ResponseEntity.ok(ticketService.getAllTicketsByPriority(priority));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponseDto>> searchTickets(@RequestParam String keyword) {
        return ResponseEntity.ok(ticketService.searchTickets(keyword));
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<List<TicketResponseDto>> advancedSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ResponseEntity.ok(ticketService.advancedSearch(keyword, status, priority));
    }

    @PutMapping("/{id}/assign/{userId}")
    public ResponseEntity<TicketResponseDto> assignTicketToUser(@PathVariable Long id, @PathVariable UUID userId) {
        return ResponseEntity.ok(ticketService.assignTicketToUser(id, userId));
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<TicketResponseDto>> getTicketsByRequester(@PathVariable UUID requesterId) {
        return ResponseEntity.ok(ticketService.getTicketsByRequester(requesterId));
    }

    @PostMapping("/{ticketId}/comment")
    public ResponseEntity<CommentResponseDto> addCommentToTicket(
            @PathVariable Long ticketId,
            @RequestParam UUID authorId,
            @RequestBody CommentRequestDto commentRequest) {
        return ResponseEntity.ok(commentService.addCommentToTicket(ticketId, authorId, commentRequest));
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getCommentsForTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId));
    }
}
