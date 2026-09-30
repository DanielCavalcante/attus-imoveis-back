package com.example.demo.dtos;

import java.time.LocalDateTime;

import com.example.demo.model.SupportRequest;

/** Linha da fila de apoio: o que a equipe precisa para ligar para alguem. */
public record BackofficeSupportListDTO(
    Long id,
    Long announcementId,
    String announcementTitle,
    String announcementCity,
    String solicitanteNome,
    String solicitanteEmail,
    String solicitanteTelefone,
    String mensagem,
    LocalDateTime createdAt
) {

    public static BackofficeSupportListDTO fromEntity(SupportRequest r) {
        return new BackofficeSupportListDTO(
            r.getId(),
            r.getAnnouncement().getId(),
            r.getAnnouncement().getTitle(),
            r.getAnnouncement().getCity(),
            r.getUser().getFullname(),
            r.getUser().getEmail(),
            r.getUser().getPhone(),
            r.getMensagem(),
            r.getCreatedAt()
        );
    }
}
