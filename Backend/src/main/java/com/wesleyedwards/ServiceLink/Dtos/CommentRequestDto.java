package com.wesleyedwards.ServiceLink.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequestDto(
        @NotBlank(message = "Content is required")
        @Size(max = 500, message = "Comment cannot exceed 500 characters")
        String content
) {}
