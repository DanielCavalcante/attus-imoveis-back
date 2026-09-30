package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.integracoes.ConsultaDeCep;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.CargaDeBairros;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 19-bairro-derivado-do-cep. Cada teste cita o criterio que fecha. */
@Import(BairroDoAnuncioSpecTest.CepProgramavel.class)
class BairroDoAnuncioSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(20000);

    /**
     * O teste nao pode depender de rede: a interface existe justamente para
     * isso. Este duble resolve o que o teste mandar resolver, e pode ser
     * posto fora do ar para provar C5.
     */
    @TestConfiguration
    static class CepProgramavel {

        @Bean
        @Primary
        ConsultaDeCep consultaDeCep() {
            return new Duble();
        }
    }

    static class Duble implements ConsultaDeCep {

        final Map<String, String> bairros = new HashMap<>();
        boolean foraDoAr = false;

        @Override
        public Resultado consultar(String cep) {
            if (foraDoAr) {
                // O contrato diz que nunca lanca: quem trata a queda e aqui.
                return Resultado.indisponivel();
            }

            // A chave e normalizada dos dois lados: desde a spec 37 quem
            // pergunta ja manda so os digitos, e o teste escreve o CEP como
            // uma pessoa escreveria, com hifen.
            String bairro = bairros.entrySet().stream()
                .filter(e -> e.getKey().replaceAll("[^0-9]", "").equals(cep))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

            if (bairro == null) {
                return Resultado.naoEncontrado();
            }

            return Resultado.encontrado(new com.example.demo.dtos.EnderecoDTO(
                cep, "Rua do duble", bairro, "João Pessoa", "PB"
            ));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.example.demo.repositories.EnderecoRepository enderecoRepository;

    @Autowired
    private ConsultaDeCep consultaDeCep;

    @Autowired
    private CargaDeBairros cargaDeBairros;

    private Duble cep;
    private String dono;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        userRepository.deleteAll();
        // Desde a spec 37 o que ja foi resolvido fica guardado em tabela, e
        // isso atravessa testes: sem limpar, o CEP resolvido por um teste
        // anterior responderia sem o duble ser consultado.
        enderecoRepository.deleteAll();

        cep = (Duble) consultaDeCep;
        cep.bairros.clear();
        cep.foraDoAr = false;

        dono = cadastrarPj();
        liberarParaAnunciar(dono);
    }

    // ---------- C1 a C5: derivacao ----------

    @Test
    @DisplayName("C1 - CEP que resolve grava o bairro sem o anunciante digitar")
    void c1() throws Exception {
        cep.bairros.put("58038-000", "Manaíra");

        criar("Apartamento", "João Pessoa", "58038-000", null);

        assertThat(announcementRepository.findAll().get(0).getNeighborhood())
            .isEqualTo("Manaíra");
    }

    @Test
    @DisplayName("C3 - bairro digitado prevalece sobre o resolvido pelo CEP")
    void c3() throws Exception {
        cep.bairros.put("58038-000", "Manaíra");

        criar("Apartamento", "João Pessoa", "58038-000", "Tambaú");

        assertThat(announcementRepository.findAll().get(0).getNeighborhood())
            .isEqualTo("Tambaú");
    }

    @Test
    @DisplayName("C4 - CEP que nao resolve nao impede a publicacao")
    void c4() throws Exception {
        criar("Apartamento", "João Pessoa", "58000-000", null);

        assertThat(announcementRepository.findAll())
            .singleElement()
            .satisfies(a -> assertThat(a.getNeighborhood()).isNull());
    }

    @Test
    @DisplayName("C5 - servico fora do ar nao impede a publicacao")
    void c5() throws Exception {
        cep.foraDoAr = true;

        criar("Apartamento", "João Pessoa", "58038-000", null);

        assertThat(announcementRepository.findAll())
            .singleElement()
            .satisfies(a -> assertThat(a.getNeighborhood()).isNull());
    }

    // ---------- C6 a C9: filtro ----------

    @Test
    @DisplayName("C6 - anuncio sem bairro nao aparece em filtro de bairro")
    void c6() throws Exception {
        cep.bairros.put("58038-000", "Manaíra");

        criar("Com bairro", "João Pessoa", "58038-000", null);
        criar("Sem bairro", "João Pessoa", "58000-000", null);

        mockMvc.perform(get("/api/v1/announcements").param("neighborhood", "Manaíra"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Com bairro"));
    }

    @Test
    @DisplayName("C7 - neighborhood devolve apenas anuncios daquele bairro")
    void c7() throws Exception {
        criar("No Manaira", "João Pessoa", "58038-000", "Manaíra");
        criar("No Bessa", "João Pessoa", "58038-000", "Bessa");

        mockMvc.perform(get("/api/v1/announcements").param("neighborhood", "Manaíra"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].neighborhood").value("Manaíra"));
    }

    @Test
    @DisplayName("C8 - neighborhood sem acento encontra o bairro acentuado")
    void c8() throws Exception {
        criar("No Manaira", "João Pessoa", "58038-000", "Manaíra");

        mockMvc.perform(get("/api/v1/announcements").param("neighborhood", "manaira"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("C9 - neighborhood combinado com city devolve so a intersecao")
    void c9() throws Exception {
        criar("JP Centro", "João Pessoa", "58038-000", "Centro");
        criar("Recife Centro", "Recife", "50000-000", "Centro");

        mockMvc.perform(get("/api/v1/announcements")
                .param("city", "João Pessoa")
                .param("neighborhood", "Centro"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].city").value("João Pessoa"));
    }

    // ---------- C10 a C13: as listas ----------

    /*
     * Estes tres passaram a usar Recife, e a troca tem motivo.
     *
     * A spec 25 deu catalogo a Joao Pessoa: a rota daquela cidade agora devolve
     * os 64 bairros do municipio, nao o distinct sobre os anuncios. O
     * comportamento que estes criterios descrevem continua vivo -- e o que a
     * spec 25 garante em C8 -- mas so onde nao ha catalogo. Mantê-los em Joao
     * Pessoa exigiria manter as duas regras ao mesmo tempo na mesma cidade.
     */

    @Test
    @DisplayName("C10 - existe rota que lista os bairros de uma cidade")
    void c10() throws Exception {
        criar("Um", "Recife", "50000-000", "Boa Viagem");

        mockMvc.perform(get("/api/v1/announcements/bairros").param("city", "Recife"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("Boa Viagem"));
    }

    @Test
    @DisplayName("C11 (spec 25) - sem catalogo, a lista traz so bairros com anuncio")
    void c11() throws Exception {
        criar("Um", "Recife", "50000-000", "Boa Viagem");
        criar("Dois", "João Pessoa", "58038-000", "Manaíra");

        mockMvc.perform(get("/api/v1/announcements/bairros").param("city", "Recife"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value("Boa Viagem"));
    }

    @Test
    @DisplayName("C12 - a lista nao repete bairro")
    void c12() throws Exception {
        criar("Um", "Recife", "50000-000", "Boa Viagem");
        criar("Dois", "Recife", "50000-000", "Boa Viagem");
        criar("Tres", "Recife", "50000-000", "Espinheiro");

        mockMvc.perform(get("/api/v1/announcements/bairros").param("city", "Recife"))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("C13 - a rota de cidades traz apenas cidades com anuncio")
    void c13() throws Exception {
        criar("Um", "João Pessoa", "58038-000", "Manaíra");
        criar("Dois", "João Pessoa", "58038-000", "Bessa");

        mockMvc.perform(get("/api/v1/announcements/cidades"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value("João Pessoa"));
    }

    // ---------- C14 e C15: a carga ----------

    @Test
    @DisplayName("C14 - a carga preenche o bairro dos anuncios ja publicados")
    void c14() throws Exception {
        criar("Antigo", "João Pessoa", "58038-000", null);

        assertThat(announcementRepository.findAll().get(0).getNeighborhood()).isNull();

        cep.bairros.put("58038-000", "Manaíra");

        int preenchidos = cargaDeBairros.executar();

        assertThat(preenchidos).isEqualTo(1);
        assertThat(announcementRepository.findAll().get(0).getNeighborhood())
            .isEqualTo("Manaíra");
    }

    @Test
    @DisplayName("C15 - anuncio cujo CEP nao resolve continua sem bairro depois da carga")
    void c15() throws Exception {
        criar("Sem CEP util", "João Pessoa", "58000-000", null);

        int preenchidos = cargaDeBairros.executar();

        assertThat(preenchidos).isZero();
        assertThat(announcementRepository.findAll().get(0).getNeighborhood()).isNull();
    }

    // ---------- apoio ----------

    private String cadastrarPj() throws Exception {
        String email = "b" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Imobiliaria de teste",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "PJ",
                      "cnpj": "%014d"
                    }
                    """.formatted(
                        email,
                        String.format("11%09d", SEQUENCIA.getAndIncrement()),
                        SEQUENCIA.getAndIncrement())))
            .andExpect(status().isCreated());

        return email;
    }

    private void criar(String titulo, String cidade, String cepDoImovel, String bairro)
        throws Exception {

        String campoBairro = bairro == null
            ? ""
            : "\"neighborhood\": \"%s\",".formatted(bairro);

        mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "description": "Anuncio da suite",
                      "image": "/api/v1/imagens/foto-de-teste.png",
                      "price": 300000,
                      "city": "%s",
                      "state": "PB",
                      "street": "Rua do Sol",
                      "streetNumber": 100,
                      "cep": "%s",
                      %s
                      "propertyType": "APARTMENT",
                      "reason": "SALE",
                      "rooms": 2,
                      "bathRooms": 1,
                      "area": 70
                    }
                    """.formatted(titulo, cidade, cepDoImovel, campoBairro)))
            .andExpect(status().isOk());
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
