package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkAssignDto(@NotEmpty List<Long> ticketIds, @NotNull UUID userId) {}
