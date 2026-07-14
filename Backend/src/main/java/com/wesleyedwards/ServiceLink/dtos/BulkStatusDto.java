package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkStatusDto(@NotEmpty List<Long> ticketIds, @NotNull TicketStatus status) {}
