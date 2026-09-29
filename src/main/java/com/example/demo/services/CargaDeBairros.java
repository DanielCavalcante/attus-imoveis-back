package com.example.demo.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.integracoes.ConsultaDeCep;
import com.example.demo.model.Announcement;
import com.example.demo.repositories.AnnouncementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Preenche o bairro dos anuncios publicados antes de o campo existir.
 *
 * Nao e migracao: Flyway nao faz chamada HTTP. A V5 cria as colunas e esta
 * rotina resolve o bairro a partir do CEP que cada anuncio ja tem gravado --
 * por isso ninguem precisa preencher nada a mao.
 *
 * Roda uma vez, no start, e so quando ligada por propriedade. Anuncio cujo CEP
 * nao resolve continua sem bairro: inventar valor seria pior que a ausencia,
 * porque o filtro passaria a mentir sobre onde o imovel fica.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CargaDeBairros {

    private final AnnouncementRepository repository;
    private final ConsultaDeCep consultaDeCep;

    @Value("${attus.bairros.carga-inicial:false}")
    private boolean ligada;

    @EventListener(ApplicationReadyEvent.class)
    public void aoSubir() {
        if (!ligada) {
            return;
        }

        int preenchidos = executar();

        log.info("Carga de bairros: {} anuncios preenchidos", preenchidos);
    }

    /** Devolve quantos ganharam bairro. Idempotente: so olha quem esta sem. */
    @Transactional
    public int executar() {
        List<Announcement> semBairro = repository.findByNeighborhoodIsNull();
        int preenchidos = 0;

        for (Announcement anuncio : semBairro) {
            String bairro = consultaDeCep.bairroDe(anuncio.getCep()).orElse(null);

            if (bairro != null) {
                anuncio.setNeighborhood(bairro);
                repository.save(anuncio);
                preenchidos++;
            }
        }

        return preenchidos;
    }
}
