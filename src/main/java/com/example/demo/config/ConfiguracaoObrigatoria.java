package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * O que precisa estar configurado para a aplicacao ter o direito de subir.
 *
 * Falhar na subida e o ponto, nao efeito colateral: um padrao de
 * desenvolvimento que sobrevive ao deploy nao avisa -- ele funciona, e o
 * defeito so aparece quando alguem percebe que a chave de assinatura e
 * publica. Sobe quebrado e barulhento, ou nao sobe.
 *
 * A ausencia das variaveis do datasource e do bucket ja derruba na resolucao
 * do `application.properties`, antes daqui. O que sobra para este ponto e o
 * que tem forma, e nao so presenca.
 */
@Component
public class ConfiguracaoObrigatoria {

    /** Abaixo disto o HS256 do JJWT recusa a chave, e recusa tarde: no primeiro login. */
    private static final int MINIMO_DE_BYTES = 32;

    private final String segredo;

    public ConfiguracaoObrigatoria(@Value("${jwt.secret}") String segredo) {
        this.segredo = segredo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void conferir() {
        int bytes = segredo == null ? 0 : segredo.getBytes().length;

        if (bytes < MINIMO_DE_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET precisa ter no mínimo %d bytes; recebeu %d."
                    .formatted(MINIMO_DE_BYTES, bytes)
            );
        }
    }
}
