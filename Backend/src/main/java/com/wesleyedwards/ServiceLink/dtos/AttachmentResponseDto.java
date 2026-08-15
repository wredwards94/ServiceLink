package com.wesleyedwards.ServiceLink.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachmentResponseDto(Long id,
                                    String filename,
                                    String contentType,
                                    long size,
                                    UUID uploader,
                                    @JsonFormat(pattern = "MM/dd/yyyy hh:mm a") LocalDateTime createdAt) {
}
