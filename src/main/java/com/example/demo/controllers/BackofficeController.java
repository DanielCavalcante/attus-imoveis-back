package com.example.demo.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.BackofficeCompanyListDTO;
import com.example.demo.dtos.CompanyDetailDTO;
import com.example.demo.dtos.CompanyRejectionDTO;
import com.example.demo.dtos.PlanCreateDTO;
import com.example.demo.dtos.PlanDTO;
import com.example.demo.dtos.PlanUpdateDTO;
import com.example.demo.dtos.ImpactoDoPlanoDTO;
import com.example.demo.dtos.LiberacaoDePlanoDTO;
import com.example.demo.dtos.SubscriptionDTO;
import com.example.demo.dtos.BackofficeSupportListDTO;
import com.example.demo.dtos.SupportRequestDTO;
import com.example.demo.services.CompanyService;
import com.example.demo.services.SupportRequestService;
import com.example.demo.services.PlanService;
import com.example.demo.services.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Painel da equipe interna. O prefixo esta fechado por papel INTERNO no
 * SecurityConfig desde a spec 01, entao nenhum metodo aqui repete a checagem.
 */
@Tag(name = "Backoffice")
@RestController
@RequestMapping("/api/v1/backoffice")
@RequiredArgsConstructor
public class BackofficeController {

    private final CompanyService companyService;
    private final PlanService planService;
    private final SupportRequestService supportRequestService;
    private final SubscriptionService subscriptionService;

    @Operation(summary = "Confirma acesso interno")
    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fila de empresas aguardando analise")
    @GetMapping("/companies")
    public ResponseEntity<Page<BackofficeCompanyListDTO>> pendingAnalysis(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(companyService.findPendingAnalysis(pageable));
    }

    @Operation(summary = "Aprova a empresa")
    @PostMapping("/companies/{id}/aprovar")
    public ResponseEntity<CompanyDetailDTO> approve(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails reviewer
    ) {
        return ResponseEntity.ok(companyService.approve(id, reviewer.getUsername()));
    }

    @Operation(summary = "Reprova a empresa, com motivo")
    @PostMapping("/companies/{id}/reprovar")
    public ResponseEntity<CompanyDetailDTO> reject(
        @PathVariable Long id,
        @Valid @RequestBody CompanyRejectionDTO dto,
        @AuthenticationPrincipal UserDetails reviewer
    ) {
        return ResponseEntity.ok(
            companyService.reject(id, dto.motivo(), reviewer.getUsername())
        );
    }

    @Operation(summary = "Pedidos de apoio de venda em aberto")
    @GetMapping("/support")
    public ResponseEntity<Page<BackofficeSupportListDTO>> openSupport(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(supportRequestService.findOpenRequests(pageable));
    }

    @Operation(summary = "Conclui um pedido de apoio")
    @PostMapping("/support/{id}/concluir")
    public ResponseEntity<SupportRequestDTO> concludeSupport(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails handler
    ) {
        return ResponseEntity.ok(
            supportRequestService.conclude(id, handler.getUsername())
        );
    }

    @Operation(summary = "Todos os planos, inclusive inativos")
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> plans() {
        return ResponseEntity.ok(planService.findAll());
    }

    @Operation(summary = "Cria um plano")
    @PostMapping("/plans")
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody PlanCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(dto));
    }

    @Operation(summary = "Altera um plano")
    @PutMapping("/plans/{id}")
    public ResponseEntity<PlanDTO> updatePlan(
        @PathVariable Long id,
        @Valid @RequestBody PlanUpdateDTO dto
    ) {
        return ResponseEntity.ok(planService.update(id, dto));
    }

    @Operation(summary = "Desativa um plano, sem remove-lo")
    @PostMapping("/plans/{id}/desativar")
    public ResponseEntity<PlanDTO> deactivatePlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.deactivate(id));
    }

    /**
     * Libera um plano para a empresa, a mao.
     *
     * Existe enquanto nao ha gateway. A contrapartida e que so a equipe
     * interna libera: nao ha rota equivalente para a empresa, que estaria
     * vendendo para si mesma.
     */
    @Operation(summary = "Libera um plano para a empresa")
    @PostMapping("/companies/{id}/plano")
    public ResponseEntity<SubscriptionDTO> liberarPlano(
        @PathVariable Long id,
        @Valid @RequestBody LiberacaoDePlanoDTO dto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            subscriptionService.liberar(id, dto.planId(), userDetails.getUsername())
        );
    }

    @Operation(summary = "Plano vigente da empresa")
    @GetMapping("/companies/{id}/plano")
    public ResponseEntity<SubscriptionDTO> planoVigente(@PathVariable Long id) {
        return subscriptionService.vigenteDe(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * O que a troca causaria, para o aviso antes da confirmacao.
     *
     * Leitura e nao confirmacao: o servidor informa o impacto, quem decide e
     * quem esta olhando a tela.
     */
    @Operation(summary = "Impacto de trocar o plano da empresa")
    @GetMapping("/companies/{id}/plano/impacto")
    public ResponseEntity<ImpactoDoPlanoDTO> impactoDoPlano(
        @PathVariable Long id,
        @RequestParam Long planId
    ) {
        return ResponseEntity.ok(subscriptionService.impactoDe(id, planId));
    }
}
