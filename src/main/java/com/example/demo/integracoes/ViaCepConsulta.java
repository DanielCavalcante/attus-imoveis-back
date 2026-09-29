package com.example.demo.integracoes;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Consulta de CEP no ViaCEP, no servidor.
 *
 * No navegador ela ficaria fora de controle de tempo limite e de teste, e o
 * valor gravado precisa ser o mesmo que o filtro usa.
 *
 * A chave `attus.cep.viacep.habilitada` existe para a suite: sem ela, toda
 * criacao de anuncio de todo teste de integracao esperaria o tempo limite de
 * uma chamada de rede.
 */
@Slf4j
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
            .requestFactory(fabricaComLimite())
            .build();
    }

    @Override
    public Optional<String> bairroDe(String cep) {
        if (!habilitada || cep == null || cep.isBlank()) {
            return Optional.empty();
        }

        String limpo = cep.replaceAll("\\D", "");

        if (limpo.length() != 8) {
            return Optional.empty();
        }

        try {
            Resposta resposta = cliente.get()
                .uri("/{cep}/json/", limpo)
                .retrieve()
                .body(Resposta.class);

            if (resposta == null || resposta.bairro() == null || resposta.bairro().isBlank()) {
                return Optional.empty();
            }

            return Optional.of(resposta.bairro().trim());
        } catch (RuntimeException falha) {
            // Silencioso de proposito: o anuncio publica sem bairro. Registrar
            // em debug e nao em erro porque servico de terceiro fora do ar nao
            // e defeito deste sistema.
            log.debug("CEP {} nao resolveu: {}", limpo, falha.getMessage());

            return Optional.empty();
        }
    }

    private org.springframework.http.client.ClientHttpRequestFactory fabricaComLimite() {
        var fabrica = new org.springframework.http.client.SimpleClientHttpRequestFactory();

        fabrica.setConnectTimeout((int) LIMITE.toMillis());
        fabrica.setReadTimeout((int) LIMITE.toMillis());

        return fabrica;
    }

    /** Cidade com CEP unico devolve bairro vazio; o campo pode vir ausente. */
    record Resposta(String bairro) {}
}
