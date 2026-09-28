package com.example.demo.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.AnnouncementListDTO;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.Announcement;
import com.example.demo.model.AnnouncementView;
import com.example.demo.model.Favorite;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.AnnouncementViewRepository;
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/** O que a conta guardou: favoritos e histórico de visita. */
@Service
@RequiredArgsConstructor
public class UserLibraryService {

    private final FavoriteRepository favoriteRepository;
    private final AnnouncementViewRepository viewRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    /**
     * Idempotente de proposito: o botao de favoritar e um alternador, e duplo
     * clique nao e erro do usuario.
     */
    @Transactional
    public void favorite(Long announcementId, String email) {
        if (favoriteRepository.findByUserEmailAndAnnouncementId(email, announcementId).isPresent()) {
            return;
        }

        Favorite favorite = new Favorite();

        favorite.setUser(requireUser(email));
        favorite.setAnnouncement(requireAnnouncement(announcementId));

        favoriteRepository.save(favorite);
    }

    @Transactional
    public void unfavorite(Long announcementId, String email) {
        favoriteRepository.findByUserEmailAndAnnouncementId(email, announcementId)
            .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementListDTO> favorites(String email) {
        return favoriteRepository.findAnnouncementsByUserEmail(email)
            .stream()
            .map(AnnouncementListDTO::fromEntity)
            .toList();
    }

    /**
     * Visita repetida atualiza o instante em vez de criar linha.
     *
     * Chamado a partir da leitura publica do anuncio: se o email for nulo, nao
     * ha sessao e nada e gravado. A leitura do anuncio nunca falha por isto.
     */
    @Transactional
    public void registerView(Long announcementId, String email) {
        if (email == null) {
            return;
        }

        AnnouncementView view = viewRepository
            .findByUserEmailAndAnnouncementId(email, announcementId)
            .orElseGet(() -> {
                AnnouncementView nova = new AnnouncementView();

                nova.setUser(requireUser(email));
                nova.setAnnouncement(requireAnnouncement(announcementId));

                return nova;
            });

        view.setViewedAt(LocalDateTime.now());

        viewRepository.save(view);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementListDTO> history(String email) {
        return viewRepository.findAnnouncementsByUserEmail(email)
            .stream()
            .map(AnnouncementListDTO::fromEntity)
            .toList();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private Announcement requireAnnouncement(Long id) {
        return announcementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado"));
    }
}
