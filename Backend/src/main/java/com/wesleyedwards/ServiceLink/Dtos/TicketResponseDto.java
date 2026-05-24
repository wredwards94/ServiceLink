package com.wesleyedwards.ServiceLink.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketResponseDto(Long id,
                                String title,
                                String description,
                                TicketStatus status,
                                TicketPriority priority,
                                String category,
                                UUID assignedTo,
                                UUID requester,
                                @JsonFormat(pattern = "MM/dd/yyyy hh:mm a") LocalDateTime createdAt,
                                @JsonFormat(pattern = "MM/dd/yyyy hh:mm a") LocalDateTime updatedAt,
                                List<CommentResponseDto> comments) {
}