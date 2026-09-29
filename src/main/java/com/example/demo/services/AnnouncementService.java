package com.example.demo.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.example.demo.enums.AnalysisStatus;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.PublicacaoBloqueadaException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.mappers.AnnouncementMapper;
import com.example.demo.model.Announcement;
import com.example.demo.model.Company;
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
    private final com.example.demo.integracoes.ConsultaDeCep consultaDeCep;
    private final com.example.demo.repositories.NeighborhoodRepository neighborhoodRepository;
    private final com.example.demo.repositories.CompanyRepository companyRepository;
    private final com.example.demo.repositories.SubscriptionRepository subscriptionRepository;

    /** Regra de negocio em propriedade: mudar o limite nao pode exigir deploy de codigo. */
    @Value("${attus.quota.pf.ativos:3}")
    private int limiteDeAtivosPf;

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
            filtro.bairro(),
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

        // Antes de qualquer gravacao: recusa nao pode deixar rastro.
        ensurePodePublicar(user);
        ensureQuota(user);

        Announcement entity = mapper.toEntity(dto);
        entity.setUser(user);
        resolverBairro(entity, dto.neighborhood(), dto.cep());

        Announcement saved = repository.save(entity);

        return mapper.toDetailDTO(saved);
    }

    /**
     * Bairro informado pelo anunciante prevalece sobre o resolvido pelo CEP.
     *
     * Cidade com CEP unico devolve bairro vazio ou errado, e quem conhece o
     * endereco e quem mora nele. Sem informado e sem resolvido, o anuncio
     * publica sem bairro: ele so nao responde a filtro de bairro.
     */
    private void resolverBairro(Announcement entity, String informado, String cep) {
        if (informado != null && !informado.isBlank()) {
            entity.setNeighborhood(informado.trim());
            return;
        }

        entity.setNeighborhood(consultaDeCep.bairroDe(cep).orElse(null));
    }

    /**
     * Saldo da conta, contando anuncios no ar.
     *
     * Contar e de todo mundo; limitar e que varia. A PF tem o teto da spec 06,
     * em propriedade; a PJ tem o teto do plano vigente (spec 30). Plano sem
     * limite devolve saldo sem limite, em vez de um numero inventado.
     */
    @Transactional(readOnly = true)
    public AnnouncementQuotaDTO quotaOf(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        long ativos = ativosDe(email);

        AnnouncementQuotaDTO saldo = limiteDe(user)
            .map(limite -> AnnouncementQuotaDTO.de(limite, ativos))
            .orElseGet(() -> AnnouncementQuotaDTO.semLimite(ativos));

        // O saldo e verdadeiro mesmo com a publicacao trancada, e continua
        // sendo entregue: quem espera aprovacao tem direito de ver quantos
        // anuncios mantem no ar.
        return bloqueioDe(user)
            .map(bloqueio -> saldo.bloqueadaPor(bloqueio.codigo()))
            .orElse(saldo);
    }

    /** O que tranca a publicacao: o codigo que a tela le, a frase que a pessoa le. */
    private record Bloqueio(String codigo, String mensagem) {}

    /**
     * O que impede esta conta de publicar agora, se e que algo impede.
     *
     * Uma fonte so para os portoes e para o aviso: a tela pergunta por aqui e
     * o `create` recusa por aqui, entao os dois nunca discordam.
     *
     * Sao dois portoes em sequencia, e a ordem importa -- empresa reprovada
     * ouve sobre a reprovacao, nao sobre plano, porque contratar plano nao
     * resolveria o caso dela.
     */
    private Optional<Bloqueio> bloqueioDe(User user) {
        if (user.getAccountType() != AccountType.PJ) {
            return Optional.empty();
        }

        // Empresa que nem chegou a existir conta como NAO_ENVIADO: ausencia de
        // dossie e o mesmo que dossie nao enviado, e um ramo a parte diria a
        // mesma coisa duas vezes.
        AnalysisStatus estado = companyRepository.findByUserId(user.getId())
            .map(Company::getAnalysisStatus)
            .orElse(AnalysisStatus.NAO_ENVIADO);

        if (estado != AnalysisStatus.APROVADA) {
            return Optional.of(new Bloqueio(
                "EMPRESA_" + estado.name(),
                "Cadastro da empresa em \"%s\". A publicação é liberada após a aprovação."
                    .formatted(estado.getDescription())
            ));
        }

        if (subscriptionRepository.vigenteDe(user.getEmail()).isEmpty()) {
            return Optional.of(new Bloqueio(
                "SEM_PLANO",
                "Nenhum plano ativo nesta conta. Fale com a equipe para liberar um plano."
            ));
        }

        return Optional.empty();
    }

    /**
     * Empresa sem aprovacao, ou sem plano, nao publica.
     *
     * Ate a spec 28 nada consultava a analise na hora de anunciar: a fila do
     * backoffice existia, aprovava e reprovava, e nao mandava em nada -- a
     * empresa publicava igual no dia seguinte ao "reprovada". E ate a spec 30
     * os planos eram catalogo sem assinante.
     */
    private void ensurePodePublicar(User user) {
        bloqueioDe(user).ifPresent(bloqueio -> {
            // A mensagem nomeia a causa: "nao autorizado" mandaria a pessoa
            // procurar problema de login, que nao e o caso.
            throw new PublicacaoBloqueadaException(bloqueio.mensagem());
        });
    }

    /**
     * O teto de anuncios no ar desta conta, quando ela tem um.
     *
     * Vazio significa sem teto, nao teto zero. A PF le a propriedade da spec
     * 06; a PJ le o limite do plano vigente, e plano de limite nulo e
     * ilimitado por definicao da spec 07.
     */
    private Optional<Integer> limiteDe(User user) {
        if (user.getAccountType() != AccountType.PJ) {
            return Optional.of(limiteDeAtivosPf);
        }

        return subscriptionRepository.vigenteDe(user.getEmail())
            .map(assinatura -> assinatura.getPlan().getLimiteAnuncios());
    }

    private void ensureQuota(User user) {
        limiteDe(user).ifPresent(limite -> {
            if (ativosDe(user.getEmail()) >= limite) {
                // 409 e nao 403: o pedido e legitimo e a conta pode publicar, o
                // que falta e vaga -- e vaga se abre inativando ou excluindo.
                throw new ConflictException(
                    "Limite de %d anuncios no ar atingido".formatted(limite)
                );
            }
        });
    }

    /** No ar agora, nao criados no mes: inativar e excluir devolvem a vaga. */
    private long ativosDe(String email) {
        return repository.countByUserEmailAndActiveTrue(email);
    }

    @Transactional
    public void inativar(@NonNull Long id, String email) {
        Announcement anuncio = doDono(id, email);

        anuncio.setActive(false);
        repository.save(anuncio);
    }

    @Transactional
    public void reativar(@NonNull Long id, String email) {
        Announcement anuncio = doDono(id, email);

        if (!anuncio.isActive()) {
            // Voltar ao ar e publicar: o mesmo portao, senao a empresa sem
            // aprovacao publicaria inativando antes e reativando depois.
            ensurePodePublicar(anuncio.getUser());
            ensureQuota(anuncio.getUser());
        }

        anuncio.setActive(true);
        repository.save(anuncio);
    }

    @Transactional
    public void excluir(@NonNull Long id, String email) {
        repository.delete(doDono(id, email));
    }

    /**
     * Carrega o anuncio exigindo que ele seja de quem pediu.
     *
     * 404 e nao 403 para quem nao e dono: responder "existe, mas nao e seu"
     * conta ao curioso que aquele id existe.
     */
    private Announcement doDono(@NonNull Long id, String email) {
        return repository.findById(id)
            .filter(anuncio -> anuncio.getUser().getEmail().equals(email))
            .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado"));
    }

    /**
     * Editar exige ser dono.
     *
     * Ate aqui este metodo recebia o e-mail da sessao e nao o usava: fazia
     * findById e gravava, entao qualquer conta autenticada editava anuncio de
     * qualquer outra, com a rota no ar. A spec 22 fechou o mesmo padrao em
     * inativar, reativar e excluir e deixou este de fora por escopo estrito.
     */
    @SuppressWarnings("null")
    @Transactional
    public AnnouncementDetailDTO edit(@NonNull Long id, AnnouncementUpdateDTO dto, String email) {
        Announcement entity = doDono(id, email);
        mapper.updateEntity(dto, entity);
        resolverBairro(entity, dto.neighborhood(), dto.cep());
        Announcement saved = repository.save(entity);

        return mapper.toDetailDTO(saved);
    }

    /**
     * As cidades oferecidas: as do acervo mais as que tem catalogo de bairros.
     *
     * A regra e a mesma de `bairrosDe`, um nivel acima. So o acervo deixava de
     * fora a cidade em que o produto comeca -- Joao Pessoa tem catalogo e nao
     * tinha anuncio, entao quem abria o filtro nao achava a propria cidade.
     *
     * A juncao e por forma normalizada para "João Pessoa" nao aparecer duas
     * vezes quando a cidade passar a ter anuncio; quem ja estava na lista
     * mantem a grafia que o acervo guardou.
     */
    @Transactional(readOnly = true)
    public List<String> cidades() {
        List<String> doAcervo = repository.cidades();

        Set<String> jaListadas = doAcervo.stream()
            .map(Announcement::normalizar)
            .collect(Collectors.toCollection(HashSet::new));

        List<String> todas = new ArrayList<>(doAcervo);

        for (String comCatalogo : neighborhoodRepository.cidades()) {
            if (jaListadas.add(Announcement.normalizar(comCatalogo))) {
                todas.add(comCatalogo);
            }
        }

        // Pela forma normalizada, pelo mesmo motivo do catalogo de bairros: sob
        // colacao C o acento joga a cidade acentuada para o fim da lista.
        todas.sort(Comparator.comparing(Announcement::normalizar));

        return todas;
    }

    /**
     * Os bairros de uma cidade: o catalogo quando existe, o acervo quando nao.
     *
     * Com catalogo, a lista e a cidade inteira -- inclusive bairro onde ainda
     * nao ha imovel, e isso e o ponto: quem procura descobre que o bairro dele
     * existe no produto, em vez de concluir que o site nao cobre a regiao.
     *
     * Sem catalogo vale o distinct sobre os anuncios, que e o que a spec 19
     * fixou. E o que faz esta mudanca nao quebrar nenhuma outra cidade.
     */
    @Transactional(readOnly = true)
    public List<String> bairrosDe(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            return List.of();
        }

        String normalizada = Announcement.normalizar(cidade.trim());

        List<String> catalogo = neighborhoodRepository.nomesDe(normalizada);

        return catalogo.isEmpty() ? repository.bairrosDe(normalizada) : catalogo;
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementListDTO> findByUser(String email, Pageable pageable) {
        return repository.findByUserEmail(email, pageable).map(AnnouncementListDTO::fromEntity);
    }

    /**
     * Detalhe do anuncio. Fora do ar, so o dono ve.
     *
     * Link antigo de anuncio inativado nao pode continuar servindo a pagina
     * como se nada tivesse mudado; 404 e nao 403 pelo mesmo motivo de doDono.
     */
    @Transactional(readOnly = true)
    public AnnouncementDetailDTO findOne(@NonNull Long id, String email) {
        Announcement announcement = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado com id: " + id));

        boolean ehDono = email != null && announcement.getUser().getEmail().equals(email);

        if (!announcement.isActive() && !ehDono) {
            throw new ResourceNotFoundException("Anúncio não encontrado com id: " + id);
        }

        return mapper.toDetailDTO(announcement);
    }

}
