package com.example.demo.integracoes;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.demo.dtos.EnderecoDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Consulta de CEP no ViaCEP.
 *
 * No servidor e nao no navegador: la ela ficaria fora de controle de tempo
 * limite e de teste, e o valor gravado precisa ser o mesmo que o filtro usa.
 *
 * A chave `attus.cep.viacep.habilitada` existe para a suite: sem ela, toda
 * criacao de anuncio de todo teste de integracao esperaria o tempo limite de
 * uma chamada de rede.
 */
@Slf4j
@Order(1)
@Component
public class ViaCepConsulta implements ConsultaDeCep {

    private static final Duration LIMITE = Duration.ofSeconds(2);

    private final RestClient cliente;
    private final boolean habilitada;

    public ViaCepConsulta(
        RestClient.Builder builder,
        @Value("${attus.cep.viacep.habilitada:true}") boolean habilitada
    ) {
        this.habilitada = habilitada;
        this.cliente = builder
            .baseUrl("https://viacep.com.br/ws")
            .requestFactory(Limites.fabricaCom(LIMITE))
            .build();
    }

    @Override
    public Resultado consultar(String cep) {
        if (!habilitada) {
            return Resultado.indisponivel();
        }

        try {
            Resposta resposta = cliente.get()
                .uri("/{cep}/json/", cep)
                .retrieve()
                .body(Resposta.class);

            // O ViaCEP responde 200 com `"erro": "true"` para CEP inexistente,
            // em vez de 404: sem olhar este campo, CEP inventado viraria
            // endereco de campos nulos.
            if (resposta == null || Boolean.parseBoolean(resposta.erro())) {
                return Resultado.naoEncontrado();
            }

            if (resposta.localidade() == null || resposta.localidade().isBlank()) {
                return Resultado.naoEncontrado();
            }

            return Resultado.encontrado(new EnderecoDTO(
                cep,
                texto(resposta.logradouro()),
                texto(resposta.bairro()),
                texto(resposta.localidade()),
                texto(resposta.uf())
            ));
        } catch (RuntimeException falha) {
            // Debug e nao erro: servico de terceiro fora do ar nao e defeito
            // deste sistema, e quem trata a queda e a cadeia.
            log.debug("ViaCEP nao respondeu para {}: {}", cep, falha.getMessage());

            return Resultado.indisponivel();
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Cidade com CEP unico devolve bairro vazio; o campo pode vir ausente. */
    record Resposta(
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        String erro
    ) {}
}
