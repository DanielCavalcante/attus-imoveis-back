package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.support.PostgresIntegrationTest;

/**
 * Cobre C8, C9 e C10 da spec 40-api-configuravel-por-ambiente.
 *
 * Com servidor de verdade, e nao MockMvc: o actuator publica os endpoints num
 * mapeamento proprio que o MockMvc nao registra, e um teste que respondesse
 * 404 ali nao diria nada sobre o que o orquestrador vai encontrar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SondasDeSaudeSpecTest extends PostgresIntegrationTest {

    @Autowired
    private TestRestTemplate cliente;

    @Test
    @DisplayName("C8 - a saude responde sem autenticacao")
    void c8() {
        ResponseEntity<String> resposta = cliente.getForEntity("/actuator/health/liveness", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).contains("UP");
    }

    @Test
    @DisplayName("C9 - a prontidao existe e e separada da saude")
    void c9() {
        // Saude diz "o processo esta de pe" e nao pode depender do banco: se
        // dependesse, uma queda do banco derrubaria e recriaria o conteiner em
        // ciclo. Prontidao diz "posso atender" e depende.
        ResponseEntity<String> resposta = cliente.getForEntity("/actuator/health/readiness", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).contains("UP");
    }

    @Test
    @DisplayName("C10 - as sondas nao expoem versao, dependencias nem configuracao")
    void c10() {
        ResponseEntity<String> saude = cliente.getForEntity("/actuator/health", String.class);

        assertThat(saude.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saude.getBody()).doesNotContain("components").doesNotContain("details");

        for (String fechado : new String[] {"env", "configprops", "beans", "mappings"}) {
            assertThat(cliente.getForEntity("/actuator/" + fechado, String.class).getStatusCode())
                .as("/actuator/%s", fechado)
                .isNotEqualTo(HttpStatus.OK);
        }
    }
}
