package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.TicketRequestDto;
import com.wesleyedwards.ServiceLink.dtos.TicketResponseDto;
import com.wesleyedwards.ServiceLink.dtos.TicketStatusUpdateDto;
import com.wesleyedwards.ServiceLink.dtos.TicketUpdateDto;
import com.wesleyedwards.ServiceLink.service.TicketService;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketResponseDto>> getAllTickets(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ticketService.getAllTickets(user));
    }



    @PostMapping("/newticket/requester")
    public ResponseEntity<TicketResponseDto> createTicket(@Valid @RequestBody TicketRequestDto createdTicket,
                                                          @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(createdTicket, user.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDto> getTicketById(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ticketService.getTicketById(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicketById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponseDto> updateTicket(@PathVariable Long id, @Valid @RequestBody TicketUpdateDto updatedTicket) {
        return ResponseEntity.ok(ticketService.updateTicket(id, updatedTicket));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketResponseDto>> getAllTicketByStatus(@PathVariable TicketStatus status) {
        return ResponseEntity.ok(ticketService.getAllTicketsByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TicketResponseDto>> getAllTicketsByPriority(@PathVariable TicketPriority priority) {
        return ResponseEntity.ok(ticketService.getAllTicketsByPriority(priority));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TicketResponseDto>> searchTickets(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(ticketService.searchTickets(keyword, pageable));
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<Page<TicketResponseDto>> advancedSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(ticketService.advancedSearch(keyword, status, priority, pageable));
    }

    @PutMapping("/{id}/assign/{userId}")
    public ResponseEntity<TicketResponseDto> assignTicketToUser(@PathVariable Long id, @PathVariable UUID userId) {
        return ResponseEntity.ok(ticketService.assignTicketToUser(id, userId));
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<TicketResponseDto>> getTicketsByRequester(@PathVariable UUID requesterId,
                                                                         @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ticketService.getTicketsByRequester(requesterId, user));
    }

    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<TicketResponseDto>> getTicketsAssignedToUser(@PathVariable UUID userId,
                                                                            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ticketService.getTicketsAssignedToUser(userId, user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDto> updateTicketStatus(@PathVariable Long id, @Valid @RequestBody TicketStatusUpdateDto status) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, status));
    }
}
