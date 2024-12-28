package com.wesleyedwards.ServiceLink.Dtos;

import java.util.List;
import java.util.UUID;

public record TicketResponseDto(Long id, String title, String description, String status, String priority,
                                String category, UUID assignedTo, UUID requester, String createdAt, String updatedAt,
                                List<CommentResponseDto> comments) {
}
