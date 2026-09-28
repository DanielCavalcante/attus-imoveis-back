package com.example.demo.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

import com.example.demo.exceptions.custom.BusinessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementListDTO;
import com.example.demo.dtos.AnnouncementQuotaDTO;
import com.example.demo.dtos.AnnouncementUpdateDTO;
import com.example.demo.dtos.SupportRequestCreateDTO;
import com.example.demo.dtos.SupportRequestDTO;
import com.example.demo.services.AnnouncementService;
import com.example.demo.services.SupportRequestService;
import com.example.demo.services.UserLibraryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Announcements")
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SupportRequestService supportRequestService;
    private final UserLibraryService userLibraryService;

    @Operation(summary = "Buscar anúncios")
    @GetMapping
    public ResponseEntity<Page<AnnouncementListDTO>> list(
        @RequestParam(required = false) String q,
        @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        validarOrdenacao(pageable);

        return ResponseEntity.ok(announcementService.findAll(q, pageable));
    }

    /**
     * Pageable aceita qualquer nome de propriedade vindo da URL. Ordenar por
     * campo arbitrario e canal lateral: da para inferir valor de coluna que
     * ninguem devia ler, comparando a ordem entre requisicoes.
     */
    private static final Set<String> ORDENACAO_PERMITIDA = Set.of("createdAt", "price");

    private void validarOrdenacao(Pageable pageable) {
        pageable.getSort().forEach(ordem -> {
            if (!ORDENACAO_PERMITIDA.contains(ordem.getProperty())) {
                throw new BusinessException(
                    "Ordenacao permitida apenas por: " + String.join(", ", ORDENACAO_PERMITIDA)
                );
            }
        });
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDetailDTO> findOne(
        @PathVariable @NonNull Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        AnnouncementDetailDTO detail = announcementService.findOne(id);

        // Rota publica: sem sessao o principal chega nulo e nada e gravado.
        userLibraryService.registerView(
            id,
            userDetails == null ? null : userDetails.getUsername()
        );

        return ResponseEntity.ok(detail);
    }

    @Operation(summary = "Favorita este anuncio")
    @PostMapping("/{id}/favorito")
    public ResponseEntity<Void> favorite(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        userLibraryService.favorite(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove este anuncio dos favoritos")
    @DeleteMapping("/{id}/favorito")
    public ResponseEntity<Void> unfavorite(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        userLibraryService.unfavorite(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Anuncios favoritados pela conta")
    @GetMapping("/me/favoritos")
    public ResponseEntity<Page<AnnouncementListDTO>> favorites(
        @AuthenticationPrincipal UserDetails userDetails,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
            userLibraryService.favorites(userDetails.getUsername(), pageable)
        );
    }

    @Operation(summary = "Anuncios vistos pela conta, do mais recente ao mais antigo")
    @GetMapping("/me/historico")
    public ResponseEntity<Page<AnnouncementListDTO>> history(
        @AuthenticationPrincipal UserDetails userDetails,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
            userLibraryService.history(userDetails.getUsername(), pageable)
        );
    }

    @Operation(summary = "Criar anúncio")
    @PostMapping
    public ResponseEntity<AnnouncementDetailDTO> create(
        @Valid @RequestBody AnnouncementCreateDTO dto, 
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(announcementService.create(dto, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementDetailDTO> update(
        @PathVariable Long id,
        @Valid @RequestBody AnnouncementUpdateDTO dto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(announcementService.edit(id, dto, userDetails.getUsername()));
    }

    // @DeleteMapping("/{id}")
    // public void delete(@PathVariable Long id) {
    //     service.delete(id);
    // }

    @Operation(summary = "Saldo de anuncios do mes da conta autenticada")
    @GetMapping("/me/quota")
    public ResponseEntity<AnnouncementQuotaDTO> myQuota(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(announcementService.quotaOf(userDetails.getUsername()));
    }

    @Operation(summary = "Pede apoio da equipe para vender este anuncio")
    @PostMapping("/{id}/apoio")
    public ResponseEntity<SupportRequestDTO> openSupport(
        @PathVariable Long id,
        @RequestBody(required = false) @Valid SupportRequestCreateDTO dto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            supportRequestService.open(id, dto, userDetails.getUsername())
        );
    }

    @Operation(summary = "Pedido de apoio vigente deste anuncio")
    @GetMapping("/{id}/apoio")
    public ResponseEntity<SupportRequestDTO> findSupport(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            supportRequestService.findOpen(id, userDetails.getUsername())
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Page<AnnouncementListDTO>> myAnnouncements(
        @AuthenticationPrincipal UserDetails userDetails,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            announcementService.findByUser(userDetails.getUsername(), pageable)
        );
    }
}