package com.wesleyedwards.ServiceLink.Dtos;

import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketRequestDto(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Status is required")
        TicketStatus status,

        @NotNull(message = "Priority is required")
        TicketPriority priority,

        @NotBlank(message = "Category is required")
        String category
) {}
