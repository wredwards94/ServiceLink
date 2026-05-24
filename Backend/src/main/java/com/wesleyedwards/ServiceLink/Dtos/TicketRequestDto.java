package com.wesleyedwards.ServiceLink.Dtos;

import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;

public record TicketRequestDto(String title, String description, TicketStatus status, TicketPriority priority, String category) {
}
