package com.example.demo.integracoes;

import java.time.Duration;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Tempo limite para as consultas de CEP.
 *
 * Vive fora dos provedores porque todos precisam do mesmo: esperar por um
 * servico fora do ar e o que a cadeia existe para evitar, e um provedor sem
 * limite seguraria os outros.
 */
final class Limites {

    private Limites() {}

    static ClientHttpRequestFactory fabricaCom(Duration limite) {
        var fabrica = new SimpleClientHttpRequestFactory();

        fabrica.setConnectTimeout((int) limite.toMillis());
        fabrica.setReadTimeout((int) limite.toMillis());

        return fabrica;
    }
}
