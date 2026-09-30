package com.example.demo.integracoes;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.demo.dtos.EnderecoDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Consulta de CEP na BrasilAPI, o segundo da cadeia.
 *
 * Ela propria consulta varios provedores por baixo: na verificacao feita
 * enquanto a spec 37 era escrita, com o ViaCEP fora do ar, respondeu pelo
 * `open-cep`. E o oposto de um ponto unico de falha, e e por isso que esta
 * aqui.
 */
@Slf4j
@Order(2)
@Component
public class BrasilApiConsulta implements ConsultaDeCep {

    private static final Duration LIMITE = Duration.ofSeconds(2);

    private final RestClient cliente;
    private final boolean habilitada;

    public BrasilApiConsulta(
        RestClient.Builder builder,
        @Value("${attus.cep.brasilapi.habilitada:true}") boolean habilitada
    ) {
        this.habilitada = habilitada;
        this.cliente = builder
            .baseUrl("https://brasilapi.com.br/api/cep/v1")
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
                .uri("/{cep}", cep)
                .retrieve()
                // 404 aqui e resposta, nao falha: o CEP nao existe, e insistir
                // com outro provedor nao mudaria isso.
                .onStatus(status -> status.value() == 404, (req, res) -> {})
                .body(Resposta.class);

            if (resposta == null || resposta.city() == null || resposta.city().isBlank()) {
                return Resultado.naoEncontrado();
            }

            return Resultado.encontrado(new EnderecoDTO(
                cep,
                texto(resposta.street()),
                texto(resposta.neighborhood()),
                texto(resposta.city()),
                texto(resposta.state())
            ));
        } catch (RuntimeException falha) {
            log.debug("BrasilAPI nao respondeu para {}: {}", cep, falha.getMessage());

            return Resultado.indisponivel();
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    record Resposta(String street, String neighborhood, String city, String state) {}
}
