package com.wesleyedwards.ServiceLink.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachmentResponseDto(long Id,
                                    String filename,
                                    String contentType,
                                    long size,
                                    UUID uploader,
                                    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
