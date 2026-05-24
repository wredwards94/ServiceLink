package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.entities.Ticket;

import java.util.List;
import java.util.UUID;

public record UserResponseDto(UUID userId,
                              ProfileResponseDto profile,
                              List<TicketResponseDto> assignedTickets,
                              List<TicketResponseDto> requestedTickets) {
}
