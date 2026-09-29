package com.example.demo.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementFilter;
import com.example.demo.dtos.AnnouncementListDTO;
import com.example.demo.dtos.AnnouncementQuotaDTO;
import com.example.demo.dtos.AnnouncementUpdateDTO;
import com.example.demo.enums.AccountType;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.mappers.AnnouncementMapper;
import com.example.demo.model.Announcement;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository repository;
    private final UserRepository userRepository;
    private final AnnouncementMapper mapper;

    /** Regra de negocio em propriedade: mudar o limite nao pode exigir deploy de codigo. */
    @Value("${attus.quota.pf.mensal:3}")
    private int limiteMensalPf;

    /**
     * Acervo publico: termo e filtros numa consulta so.
     *
     * A normalizacao de cada valor e a regra de "branco nao filtra" vivem em
     * AnnouncementFilter, onde o controlador e o teste enxergam as duas.
     */
    @Transactional(readOnly = true)
    public Page<AnnouncementListDTO> findAll(AnnouncementFilter filtro, Pageable pageable) {
        Page<Announcement> pagina = repository.filtrar(
            filtro.termo(),
            filtro.cidade(),
            filtro.precoMinimo(),
            filtro.precoMaximo(),
            filtro.tipos(),
            filtro.finalidade(),
            pageable
        );

        return pagina.map(AnnouncementListDTO::fromEntity);
    }

    @Transactional
    public AnnouncementDetailDTO create(AnnouncementCreateDTO dto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Antes de qualquer gravacao: recusa por quota nao pode deixar rastro.
        ensureQuota(user);

        Announcement entity = mapper.toEntity(dto);
        entity.setUser(user);

        Announcement saved = repository.save(entity);

        return mapper.toDetailDTO(saved);
    }

    @Transactional(readOnly = true)
    public AnnouncementQuotaDTO quotaOf(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!hasQuota(user)) {
            return AnnouncementQuotaDTO.semLimite();
        }

        return AnnouncementQuotaDTO.de(limiteMensalPf, usedThisMonth(email));
    }

    /** So a pessoa fisica tem quota gratuita; PJ publica conforme plano. */
    private boolean hasQuota(User user) {
        return user.getAccountType() == AccountType.PF;
    }

    private void ensureQuota(User user) {
        if (!hasQuota(user)) {
            return;
        }

        long usados = usedThisMonth(user.getEmail());

        if (usados >= limiteMensalPf) {
            // 409 e nao 403: o pedido e legitimo e a conta pode publicar,
            // o que falta e saldo do mes.
            throw new ConflictException(
                "Limite de %d anuncios no mes atingido".formatted(limiteMensalPf)
            );
        }
    }

    /** Mes civil, nao janela de trinta dias: "tres por mes" e o que foi combinado. */
    private long usedThisMonth(String email) {
        LocalDateTime inicioDoMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return repository.countByUserEmailAndCreatedAtGreaterThanEqual(email, inicioDoMes);
    }

    @SuppressWarnings("null")
    @Transactional
    public AnnouncementDetailDTO edit(@NonNull Long id, AnnouncementUpdateDTO dto, String email) {
        Announcement entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado"));
        mapper.updateEntity(dto, entity);
        Announcement saved = repository.save(entity);

        return mapper.toDetailDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementListDTO> findByUser(String email, Pageable pageable) {
        return repository.findByUserEmail(email, pageable).map(AnnouncementListDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public AnnouncementDetailDTO findOne(@NonNull Long id) {
        Announcement announcement = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado com id: " + id));
        return mapper.toDetailDTO(announcement);
    }

}
