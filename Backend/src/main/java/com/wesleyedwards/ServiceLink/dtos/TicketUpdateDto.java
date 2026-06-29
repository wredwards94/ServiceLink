package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;

/**
 * Partial-update DTO for PATCH /api/tickets/{id}.
 * All fields are optional; omitted fields are left unchanged by the mapper
 * (nullValuePropertyMappingStrategy = IGNORE).
 */
public record TicketUpdateDto(
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String category
) {}
