package com.example.demo.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.BackofficeSupportListDTO;
import com.example.demo.dtos.SupportRequestCreateDTO;
import com.example.demo.dtos.SupportRequestDTO;
import com.example.demo.enums.SupportStatus;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.Announcement;
import com.example.demo.model.SupportRequest;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.SupportRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportRequestService {

    private final SupportRequestRepository repository;
    private final AnnouncementRepository announcementRepository;

    @Transactional
    public SupportRequestDTO open(Long announcementId, SupportRequestCreateDTO dto, String email) {
        Announcement announcement = requireOwnedAnnouncement(announcementId, email);

        // Dois pedidos abertos para o mesmo imovel dividem o atendimento sem
        // acrescentar informacao.
        if (repository.existsByAnnouncementIdAndStatus(announcementId, SupportStatus.ABERTO)) {
            throw new ConflictException("Ja existe um pedido de apoio aberto para este anuncio");
        }

        SupportRequest request = new SupportRequest();

        request.setAnnouncement(announcement);
        request.setUser(announcement.getUser());
        request.setMensagem(dto == null ? null : dto.mensagem());
        request.setStatus(SupportStatus.ABERTO);

        return SupportRequestDTO.fromEntity(repository.save(request));
    }

    @Transactional(readOnly = true)
    public SupportRequestDTO findOpen(Long announcementId, String email) {
        requireOwnedAnnouncement(announcementId, email);

        return repository.findByAnnouncementIdAndStatus(announcementId, SupportStatus.ABERTO)
            .map(SupportRequestDTO::fromEntity)
            .orElseThrow(() ->
                new ResourceNotFoundException("Nenhum pedido de apoio aberto para este anuncio"));
    }

    @Transactional(readOnly = true)
    public Page<BackofficeSupportListDTO> findOpenRequests(Pageable pageable) {
        return repository.findAllByStatus(SupportStatus.ABERTO, pageable)
            .map(BackofficeSupportListDTO::fromEntity);
    }

    @Transactional
    public SupportRequestDTO conclude(Long requestId, String handlerEmail) {
        SupportRequest request = repository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido de apoio nao encontrado"));

        request.setStatus(SupportStatus.CONCLUIDO);
        request.setHandledBy(handlerEmail);
        request.setHandledAt(LocalDateTime.now());

        return SupportRequestDTO.fromEntity(repository.save(request));
    }

    /**
     * 403 e nao 404 para quem nao e dono: o anuncio e publico, entao esconder
     * sua existencia nao protege nada e confundiria quem errou de conta.
     */
    private Announcement requireOwnedAnnouncement(Long announcementId, String email) {
        Announcement announcement = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Anuncio nao encontrado"));

        if (!announcement.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("Este anuncio nao e desta conta");
        }

        return announcement;
    }
}
