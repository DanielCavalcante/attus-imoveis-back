package com.example.demo.services;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.ImpactoDoPlanoDTO;
import com.example.demo.dtos.SubscriptionDTO;
import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.Company;
import com.example.demo.model.Plan;
import com.example.demo.model.Subscription;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.PlanRepository;
import com.example.demo.repositories.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Liberacao de plano a mao, enquanto nao ha gateway de pagamento.
 *
 * Quem libera e a equipe interna, pela tela do backoffice. A empresa nao tem
 * rota para mudar o proprio plano: seria vender para si mesma.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final AnnouncementRepository announcementRepository;

    /**
     * Troca o plano vigente da empresa pelo informado.
     *
     * Downgrade nao inativa nem altera anuncio nenhum, e isso e decisao, nao
     * omissao: derrubar acervo de quem ja anuncia e efeito grande demais para
     * sair de uma troca de linha numa tabela. O excedente aparece no aviso, e
     * a empresa so sente o teto novo na proxima publicacao.
     */
    @Transactional
    public SubscriptionDTO liberar(Long companyId, Long planId, String liberadoPor) {
        Company empresa = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        Plan plano = planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        if (!plano.isAtivo()) {
            // Plano desativado nao se vende nem se da: a spec 07 mantem o plano
            // no banco por causa de quem ja o assina, nao para novas adesoes.
            throw new BusinessException("Plano desativado não pode ser liberado");
        }

        // A anterior vira trilha, nao lixo: e ela que conta quem liberou o que.
        repository.findByCompanyIdAndVigenteTrue(companyId).ifPresent(anterior -> {
            anterior.setVigente(false);
            repository.save(anterior);
        });

        // Antes de inserir a nova: o indice unico parcial recusa duas vigentes,
        // e sem este flush a insercao corre contra a atualizacao acima.
        repository.flush();

        Subscription nova = new Subscription();

        nova.setCompany(empresa);
        nova.setPlan(plano);
        nova.setVigente(true);
        nova.setLiberadoPor(liberadoPor);
        nova.setLiberadoEm(LocalDateTime.now());

        return SubscriptionDTO.fromEntity(repository.save(nova));
    }

    /** A tela de liberacao: empresas aprovadas, plano vigente e acervo no ar. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.example.demo.dtos.CompanyPlanListDTO>
        empresasAprovadas(org.springframework.data.domain.Pageable pageable) {

        return repository.empresasAprovadas(pageable);
    }

    /** O plano vigente da empresa, se houver. */
    @Transactional(readOnly = true)
    public Optional<SubscriptionDTO> vigenteDe(Long companyId) {
        return repository.findByCompanyIdAndVigenteTrue(companyId)
            .map(SubscriptionDTO::fromEntity);
    }

    /**
     * O que a troca causaria, para o aviso da tela.
     *
     * Leitura, nao confirmacao: quem decide e a pessoa no backoffice.
     */
    @Transactional(readOnly = true)
    public ImpactoDoPlanoDTO impactoDe(Long companyId, Long planId) {
        Company empresa = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        Plan destino = planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        long noAr = announcementRepository.countByUserEmailAndActiveTrue(
            empresa.getUser().getEmail()
        );

        Optional<Subscription> atual = repository.findByCompanyIdAndVigenteTrue(companyId);

        Integer limiteDestino = destino.getLimiteAnuncios();

        return new ImpactoDoPlanoDTO(
            noAr,
            atual.map(s -> s.getPlan().getNome()).orElse(null),
            atual.map(s -> s.getPlan().getLimiteAnuncios()).orElse(null),
            destino.getNome(),
            limiteDestino,
            // Limite nulo e ilimitado: nunca excede.
            limiteDestino != null && noAr > limiteDestino
        );
    }
}
