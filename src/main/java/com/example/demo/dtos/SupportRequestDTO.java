package com.example.demo.dtos;

import java.time.LocalDateTime;

import com.example.demo.enums.SupportStatus;
import com.example.demo.model.SupportRequest;

public record SupportRequestDTO(
    Long id,
    Long announcementId,
    String mensagem,
    SupportStatus status,
    LocalDateTime createdAt
) {

    public static SupportRequestDTO fromEntity(SupportRequest request) {
        return new SupportRequestDTO(
            request.getId(),
            request.getAnnouncement().getId(),
            request.getMensagem(),
            request.getStatus(),
            request.getCreatedAt()
        );
    }
}
