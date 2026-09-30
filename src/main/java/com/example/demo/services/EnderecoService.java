package com.example.demo.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.EnderecoDTO;
import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.exceptions.custom.ServicoIndisponivelException;
import com.example.demo.integracoes.ConsultaDeCep;
import com.example.demo.model.Endereco;
import com.example.demo.repositories.EnderecoRepository;

import lombok.RequiredArgsConstructor;

/**
 * O endereco de um CEP: primeiro o que ja se sabe, depois os provedores.
 *
 * So se consulta terceiro o que ainda nao se sabe. CEP nao muda de bairro,
 * entao o que foi resolvido uma vez vale para sempre -- e continua valendo
 * quando todos os provedores estao fora, que e o cenario que motivou isto.
 */
@Service
@RequiredArgsConstructor
public class EnderecoService {

    /**
     * Os provedores, na ordem em que sao tentados.
     *
     * A ordem vem do `@Order` de cada um. O que importa nao e qual vem antes,
     * e sim haver mais de um: ate a spec 37 o ViaCEP era ponto unico de falha.
     */
    private final List<ConsultaDeCep> provedores;

    private final EnderecoRepository repository;

    /**
     * O endereco, ou a explicacao de por que nao.
     *
     * Lanca de proposito: quem chama esta perguntando pelo endereco, e uma
     * resposta vazia esconderia a diferenca entre "este CEP nao existe" e
     * "nao consegui perguntar". Para o caminho do anuncio, que nao pode
     * quebrar por causa de terceiro, existe `bairroDe`.
     */
    @Transactional
    public EnderecoDTO buscar(String cep) {
        String limpo = normalizar(cep);
        Resolucao resolucao = resolver(limpo);

        return switch (resolucao.status()) {
            case ENCONTRADO -> resolucao.endereco();
            case NAO_ENCONTRADO -> throw new ResourceNotFoundException(
                "CEP %s não encontrado".formatted(limpo)
            );
            case INDISPONIVEL -> throw new ServicoIndisponivelException(
                "A consulta de CEP está indisponível no momento. Tente de novo em instantes."
            );
        };
    }

    /**
     * O bairro do CEP, para o caminho do anuncio.
     *
     * Nao lanca, e nao apenas por contrato: `create` roda dentro de uma
     * transacao, e excecao lancada la dentro -- mesmo capturada -- marcaria a
     * transacao para rollback e derrubaria a publicacao inteira por causa de
     * um servico de terceiro. Por isso `resolver` existe separado de `buscar`.
     *
     * A spec 19 decidiu que bairro e recorte de busca, nao dado essencial do
     * imovel. Vazio aqui significa "nao deu", sem distinguir o motivo, porque
     * para este chamador os dois motivos levam ao mesmo lugar.
     */
    @Transactional
    public Optional<String> bairroDe(String cep) {
        String limpo;

        try {
            limpo = normalizar(cep);
        } catch (BusinessException malformado) {
            return Optional.empty();
        }

        Resolucao resolucao = resolver(limpo);

        return resolucao.status() == ConsultaDeCep.Status.ENCONTRADO
            ? Optional.ofNullable(resolucao.endereco().bairro())
            : Optional.empty();
    }

    /** O resultado da resolucao, com o endereco quando ha um. */
    private record Resolucao(ConsultaDeCep.Status status, EnderecoDTO endereco) {}

    /**
     * Guardado primeiro, provedores depois. Nao lanca.
     *
     * E aqui que mora a regra central da spec 37: so se consulta terceiro o
     * que ainda nao se sabe.
     */
    private Resolucao resolver(String cep) {
        Optional<Endereco> guardado = repository.findById(cep);

        if (guardado.isPresent()) {
            return new Resolucao(ConsultaDeCep.Status.ENCONTRADO, paraDTO(guardado.get()));
        }

        boolean alguemDisseQueNaoExiste = false;

        for (ConsultaDeCep provedor : provedores) {
            ConsultaDeCep.Resultado resultado = provedor.consultar(cep);

            if (resultado.status() == ConsultaDeCep.Status.ENCONTRADO) {
                // O nome vem de quem REALMENTE respondeu: gravar o primeiro da
                // lista faria a coluna mentir no dia em que ele estivesse fora.
                EnderecoDTO salvo = guardar(
                    resultado.endereco(), cep, provedor.getClass().getSimpleName()
                );

                return new Resolucao(ConsultaDeCep.Status.ENCONTRADO, salvo);
            }

            // "Nao existe" nao encerra a busca: provedores tem bases
            // diferentes, e CEP novo chega em um antes do outro. Mas a
            // resposta fica lembrada, porque e melhor que "indisponivel" se
            // nenhum outro souber.
            if (resultado.status() == ConsultaDeCep.Status.NAO_ENCONTRADO) {
                alguemDisseQueNaoExiste = true;
            }
        }

        return new Resolucao(
            alguemDisseQueNaoExiste
                ? ConsultaDeCep.Status.NAO_ENCONTRADO
                : ConsultaDeCep.Status.INDISPONIVEL,
            null
        );
    }

    private EnderecoDTO guardar(EnderecoDTO resolvido, String cep, String provedor) {
        Endereco endereco = new Endereco();

        endereco.setCep(cep);
        endereco.setLogradouro(resolvido.logradouro());
        endereco.setBairro(resolvido.bairro());
        endereco.setCidade(resolvido.cidade());
        endereco.setUf(resolvido.uf());
        endereco.setProvedor(provedor);
        endereco.setResolvidoEm(LocalDateTime.now());

        repository.save(endereco);

        return paraDTO(endereco);
    }

    private EnderecoDTO paraDTO(Endereco endereco) {
        return new EnderecoDTO(
            endereco.getCep(),
            endereco.getLogradouro(),
            endereco.getBairro(),
            endereco.getCidade(),
            endereco.getUf()
        );
    }

    /**
     * Oito digitos, sem hifen.
     *
     * A forma normalizada e a chave da tabela: sem ela, "58038-000" e
     * "58038000" viveriam como dois CEPs e o guardado nunca acertaria.
     */
    private String normalizar(String cep) {
        String limpo = cep == null ? "" : cep.replaceAll("\\D", "");

        if (limpo.length() != 8) {
            throw new BusinessException("CEP deve ter 8 dígitos");
        }

        return limpo;
    }
}
