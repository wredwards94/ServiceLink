package com.wesleyedwards.ServiceLink.dtos;

public record AttachmentDownloadDto(String filename, String contentType, byte[] data) {
}
