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

import java.math.BigDecimal;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementFilter;
import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;
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

    /**
     * Os filtros se combinam por intersecao: cada parametro presente estreita o
     * resultado, ausente nao restringe nada.
     *
     * `type` e repetivel e vale como "qualquer um destes". Valor fora do enum
     * e recusado pelo binding do Spring, com 400.
     *
     * Faixa invertida (minPrice maior que maxPrice) devolve pagina vazia, nao
     * 400: e consulta sem resultado, nao requisicao malformada. Impedir a
     * inversao e trabalho da tela.
     */
    @Operation(summary = "Buscar anúncios")
    @GetMapping
    public ResponseEntity<Page<AnnouncementListDTO>> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String neighborhood,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) List<PropertyType> type,
        @RequestParam(required = false) ReasonType reason,
        @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        validarOrdenacao(pageable);

        AnnouncementFilter filtro =
            AnnouncementFilter.de(q, city, neighborhood, minPrice, maxPrice, type, reason);

        return ResponseEntity.ok(announcementService.findAll(filtro, pageable));
    }

    /**
     * As cidades e os bairros que existem sao os que tem anuncio, nao um
     * cadastro. E isto que faz o sistema nascer em Joao Pessoa e funcionar em
     * Belo Horizonte no dia em que alguem publicar la, sem ninguem cadastrar
     * nada.
     */
    @Operation(summary = "Cidades com anúncio publicado")
    @GetMapping("/cidades")
    public ResponseEntity<List<String>> cidades() {
        return ResponseEntity.ok(announcementService.cidades());
    }

    @Operation(summary = "Bairros com anúncio publicado numa cidade")
    @GetMapping("/bairros")
    public ResponseEntity<List<String>> bairros(@RequestParam String city) {
        return ResponseEntity.ok(announcementService.bairrosDe(city));
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
        // Rota publica: sem sessao o principal chega nulo. O e-mail vai ao
        // servico porque anuncio fora do ar so o dono ve.
        String email = userDetails == null ? null : userDetails.getUsername();

        AnnouncementDetailDTO detail = announcementService.findOne(id, email);

        userLibraryService.registerView(id, email);

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

    /**
     * As tres rotas nascem com dono verificado.
     *
     * Existia aqui um @DeleteMapping comentado chamando service.delete(id)
     * sem o e-mail da sessao: descomentado, seria qualquer conta apagando
     * anuncio de qualquer outra.
     */
    @Operation(summary = "Tira o anúncio do ar, devolvendo a vaga da quota")
    @PatchMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(
        @PathVariable @NonNull Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        announcementService.inativar(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Devolve o anúncio ao ar, se houver vaga")
    @PatchMapping("/{id}/reativar")
    public ResponseEntity<Void> reativar(
        @PathVariable @NonNull Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        announcementService.reativar(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Exclui o anúncio definitivamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
        @PathVariable @NonNull Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        announcementService.excluir(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

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