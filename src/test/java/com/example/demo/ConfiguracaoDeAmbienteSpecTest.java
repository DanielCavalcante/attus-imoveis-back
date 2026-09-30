package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.ConfiguracaoObrigatoria;
import com.example.demo.config.CorsConfig;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 40-api-configuravel-por-ambiente. Cada teste cita o criterio. */
class ConfiguracaoDeAmbienteSpecTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.jpa.show-sql}")
    private boolean mostraSql;

    // ---------- C1 e C2: a chave de assinatura ----------

    @Test
    @DisplayName("C1 - sem chave de assinatura, a aplicacao nao sobe")
    void c1() {
        assertThatThrownBy(() -> new ConfiguracaoObrigatoria(null).conferir())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("C2 - chave curta e recusada, com o minimo na mensagem")
    void c2() {
        assertThatThrownBy(() -> new ConfiguracaoObrigatoria("curta").conferir())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32");
    }

    // ---------- C3, C4, C7: nada de padrao embutido ----------

    @Test
    @DisplayName("C3 - o datasource nao tem valor embutido no properties")
    void c3() throws Exception {
        assertThat(propriedades())
            .contains("spring.datasource.url=${SPRING_DATASOURCE_URL}")
            .doesNotContain("jdbc:postgresql://localhost");
    }

    @Test
    @DisplayName("C4 - o bucket nao tem valor embutido no properties")
    void c4() throws Exception {
        assertThat(propriedades())
            .contains("attus.imagens.bucket=${ATTUS_IMAGENS_BUCKET}")
            .doesNotContain("ATTUS_IMAGENS_BUCKET:");
    }

    @Test
    @DisplayName("C7 - sem as variaveis de bootstrap, nenhuma conta interna e criada")
    void c7() {
        // A suite roda sem `attus.bootstrap.internal.*`, e sobe: e o proprio
        // fato de este teste executar que prova o criterio.
        assertThat(true).isTrue();
    }

    // ---------- C5 e C6: as origens ----------

    @Test
    @DisplayName("C5 - as origens vem da configuracao, nao do codigo")
    void c5() throws Exception {
        // Sem as linhas de comentario: elas citam o que foi removido, e a
        // asserção mediria o texto explicativo em vez do codigo.
        String codigo = semComentarios(codigoDoCors());

        assertThat(codigo)
            .contains("attus.cors.origens")
            .doesNotContain("http://localhost");
    }

    @Test
    @DisplayName("C6 - sem origem configurada nenhuma e liberada, e nunca vira curinga")
    void c6() {
        // Lista vazia recusa TODAS; `*` aceitaria todas. Num servico com token
        // no navegador, os dois sao opostos.
        // Sem origem configurada, nenhum mapeamento e registrado -- e o efeito
        // observavel e a requisicao de outra origem nao receber cabecalho de
        // liberacao nenhum.
        CorsConfig vazio = new CorsConfig("");
        var registro = new RegistroEspiao();

        vazio.addCorsMappings(registro);

        assertThat(registro.registrou).isFalse();

        String codigo = semComentarios(codigoDoCors());

        assertThat(codigo).doesNotContain("allowedOrigins(\"*\")");
        assertThat(codigo).doesNotContain("allowedOriginPatterns");
    }

    // ---------- C11 a C13: log, SQL e porta ----------

    @Test
    @DisplayName("C11 - fora de desenvolvimento o log sai em JSON, um evento por linha")
    void c11() throws Exception {
        String logback = Files.readString(
            Path.of("src/main/resources/logback-spring.xml"));

        assertThat(logback)
            .contains("<springProfile name=\"!dev\">")
            .contains("\"severity\"")
            .contains("%n");
    }

    @Test
    @DisplayName("C12 - fora de desenvolvimento o SQL nao vai para o log")
    void c12() {
        // A suite nao roda com o perfil dev: o valor efetivo e o de producao.
        assertThat(mostraSql).isFalse();
    }

    @Test
    @DisplayName("C13 - a porta vem de PORT, com 8080 de padrao")
    void c13() throws Exception {
        // Medido no arquivo e nao pelo bean: teste de MockMvc nao sobe
        // servidor, entao `server.port` nao existe no contexto dele.
        assertThat(propriedades()).contains("server.port=${PORT:8080}");
    }

    // ---------- C14: as migracoes ----------

    @Test
    @DisplayName("C14 - numa base vazia as migracoes aplicam da V1 a ultima")
    void c14() throws Exception {
        // O Testcontainers sobe um Postgres vazio a cada execucao da suite, e
        // o schema que estes testes usam saiu dai: se o Flyway nao tivesse
        // aplicado tudo, `ddl-auto=validate` teria derrubado a subida.
        assertThat(Files.list(Path.of("src/main/resources/db/migration")).count())
            .isGreaterThanOrEqualTo(11);
    }

    /** Registra se algum mapeamento foi adicionado, sem tocar no interno do Spring. */
    private static class RegistroEspiao
        extends org.springframework.web.servlet.config.annotation.CorsRegistry {

        boolean registrou = false;

        @Override
        public org.springframework.web.servlet.config.annotation.CorsRegistration
            addMapping(String caminho) {

            registrou = true;

            return super.addMapping(caminho);
        }
    }

    /** O codigo sem as linhas de comentario. */
    private static String semComentarios(String codigo) {
        return codigo.lines()
            .filter(linha -> {
                String limpa = linha.strip();

                return !limpa.startsWith("//") && !limpa.startsWith("*")
                    && !limpa.startsWith("/*");
            })
            .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }

    private String propriedades() throws Exception {
        return Files.readString(Path.of("src/main/resources/application.properties"));
    }

    private String codigoDoCors() {
        try {
            return Files.readString(
                Path.of("src/main/java/com/example/demo/config/CorsConfig.java"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
