package com.wesleyedwards.ServiceLink.Dtos;

import com.wesleyedwards.ServiceLink.Entities.Ticket;

import java.util.List;
import java.util.UUID;

public record UserResponseDto(UUID userId, ProfileResponseDto profile, List<Ticket> assignedTickets, List<Ticket> requestedTickets) {
}
