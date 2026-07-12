package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TicketStatusUpdateDto(@Valid @NotNull TicketStatus ticketStatus) {
}
