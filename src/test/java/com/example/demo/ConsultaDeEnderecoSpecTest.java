package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dtos.EnderecoDTO;
import com.example.demo.integracoes.ConsultaDeCep;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.EnderecoRepository;
import com.example.demo.repositories.SubscriptionRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 37-consulta-de-endereco-por-cep. Cada teste cita o criterio. */
@Import({
    ConsultaDeEnderecoSpecTest.Provedores.class
})
class ConsultaDeEnderecoSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(100000);

    private static final String CEP = "58038000";

    /**
     * Dois provedores programaveis, na ordem em que a cadeia os tenta.
     *
     * Sao dois de proposito: o criterio central da spec e que o primeiro cair
     * nao derrube a consulta, e isso nao se prova com um.
     */
    static class ProvedorProgramavel implements ConsultaDeCep {

        final List<String> chamadas = new ArrayList<>();
        Status resposta = Status.INDISPONIVEL;
        EnderecoDTO endereco;

        @Override
        public Resultado consultar(String cep) {
            chamadas.add(cep);

            return switch (resposta) {
                case ENCONTRADO -> Resultado.encontrado(endereco);
                case NAO_ENCONTRADO -> Resultado.naoEncontrado();
                case INDISPONIVEL -> Resultado.indisponivel();
            };
        }

        void responde(EnderecoDTO endereco) {
            this.resposta = Status.ENCONTRADO;
            this.endereco = endereco;
        }
    }

    @TestConfiguration
    static class Provedores {

        @Bean
        @Order(1)
        ProvedorProgramavel primeiro() {
            return new ProvedorProgramavel();
        }

        @Bean
        @Order(2)
        ProvedorProgramavel segundo() {
            return new ProvedorProgramavel();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProvedorProgramavel primeiro;

    @Autowired
    private ProvedorProgramavel segundo;

    private String conta;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
        enderecoRepository.deleteAll();

        primeiro.chamadas.clear();
        segundo.chamadas.clear();
        primeiro.resposta = ConsultaDeCep.Status.INDISPONIVEL;
        segundo.resposta = ConsultaDeCep.Status.INDISPONIVEL;

        conta = cadastrarPj();
        liberarParaAnunciar(conta);
    }

    private static EnderecoDTO manaira() {
        return new EnderecoDTO(CEP, "Avenida João Maurício", "Manaíra", "João Pessoa", "PB");
    }

    // ---------- C1 a C3: o que a rota responde ----------

    @Test
    @DisplayName("C1 - a rota devolve logradouro, bairro, cidade e UF")
    void c1() throws Exception {
        primeiro.responde(manaira());

        consultar(CEP)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logradouro").value("Avenida João Maurício"))
            .andExpect(jsonPath("$.bairro").value("Manaíra"))
            .andExpect(jsonPath("$.cidade").value("João Pessoa"))
            .andExpect(jsonPath("$.uf").value("PB"));
    }

    @Test
    @DisplayName("C2 - CEP que nao existe recebe 404")
    void c2() throws Exception {
        primeiro.resposta = ConsultaDeCep.Status.NAO_ENCONTRADO;
        segundo.resposta = ConsultaDeCep.Status.NAO_ENCONTRADO;

        consultar("99999999").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C3 - CEP fora do formato recebe 400")
    void c3() throws Exception {
        consultar("123").andExpect(status().isBadRequest());
    }

    // ---------- C4, C5, C8: o que ja foi resolvido ----------

    @Test
    @DisplayName("C4 - CEP ja consultado nao chama provedor nenhum")
    void c4() throws Exception {
        primeiro.responde(manaira());

        consultar(CEP).andExpect(status().isOk());

        primeiro.chamadas.clear();
        segundo.chamadas.clear();

        consultar(CEP).andExpect(status().isOk());

        assertThat(primeiro.chamadas).isEmpty();
        assertThat(segundo.chamadas).isEmpty();
    }

    @Test
    @DisplayName("C5 - a primeira consulta fica guardada")
    void c5() throws Exception {
        primeiro.responde(manaira());

        consultar(CEP).andExpect(status().isOk());

        assertThat(enderecoRepository.findById(CEP))
            .get()
            .satisfies(e -> {
                assertThat(e.getBairro()).isEqualTo("Manaíra");
                // Quem realmente respondeu, e nao o primeiro da lista.
                assertThat(e.getProvedor()).isEqualTo("ProvedorProgramavel");
            });
    }

    @Test
    @DisplayName("C8 - com todos os provedores fora, o guardado continua respondendo")
    void c8() throws Exception {
        primeiro.responde(manaira());

        consultar(CEP).andExpect(status().isOk());

        // O cenario que motivou a spec: os provedores caem depois.
        primeiro.resposta = ConsultaDeCep.Status.INDISPONIVEL;
        segundo.resposta = ConsultaDeCep.Status.INDISPONIVEL;

        consultar(CEP)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bairro").value("Manaíra"));
    }

    // ---------- C6 e C7: a cadeia ----------

    @Test
    @DisplayName("C6 - com o primeiro fora, a resposta vem do segundo")
    void c6() throws Exception {
        primeiro.resposta = ConsultaDeCep.Status.INDISPONIVEL;
        segundo.responde(manaira());

        consultar(CEP)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bairro").value("Manaíra"));

        assertThat(primeiro.chamadas).containsExactly(CEP);
        assertThat(segundo.chamadas).containsExactly(CEP);
    }

    @Test
    @DisplayName("C7 - com os dois fora e o CEP desconhecido, a rota responde 503")
    void c7() throws Exception {
        consultar(CEP).andExpect(status().isServiceUnavailable());
    }

    // ---------- C9 e C14: a rota ----------

    @Test
    @DisplayName("C9 - a rota exige autenticacao")
    void c9() throws Exception {
        mockMvc.perform(get("/api/v1/enderecos/" + CEP))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("C14 - a resposta e cacheavel pelo navegador")
    void c14() throws Exception {
        primeiro.responde(manaira());

        consultar(CEP)
            .andExpect(header().string("Cache-Control",
                org.hamcrest.Matchers.containsString("max-age")));
    }

    // ---------- C11 a C13: o caminho do anuncio ----------

    @Test
    @DisplayName("C11 - anuncio sem bairro informado deriva do CEP")
    void c11() throws Exception {
        primeiro.responde(manaira());

        publicar(null).andExpect(status().isOk());

        assertThat(announcementRepository.findAll().get(0).getNeighborhood())
            .isEqualTo("Manaíra");
    }

    @Test
    @DisplayName("C12 - com todos os provedores fora, o anuncio publica sem bairro")
    void c12() throws Exception {
        // Nenhum provedor responde e o CEP nunca foi resolvido: a publicacao
        // nao pode cair por causa disso.
        publicar(null).andExpect(status().isOk());

        assertThat(announcementRepository.findAll().get(0).getNeighborhood()).isNull();
    }

    @Test
    @DisplayName("C13 - bairro informado prevalece sobre o derivado")
    void c13() throws Exception {
        primeiro.responde(manaira());

        publicar("Bessa").andExpect(status().isOk());

        assertThat(announcementRepository.findAll().get(0).getNeighborhood())
            .isEqualTo("Bessa");
    }

    // ---------- apoio ----------

    private org.springframework.test.web.servlet.ResultActions consultar(String cep)
        throws Exception {

        return mockMvc.perform(get("/api/v1/enderecos/" + cep)
            .header("Authorization", "Bearer " + tokenDe(conta)));
    }

    private org.springframework.test.web.servlet.ResultActions publicar(String bairro)
        throws Exception {

        String campoDoBairro = bairro == null ? "" : "\"neighborhood\": \"%s\",".formatted(bairro);

        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(conta))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Casa da spec 37",
                  "description": "Anuncio da suite",
                  "image": "/api/v1/imagens/foto-de-teste.png",
                  "price": 300000,
                  "city": "João Pessoa",
                  "state": "PB",
                  %s
                  "street": "Rua do Sol",
                  "streetNumber": 100,
                  "cep": "58038-000",
                  "propertyType": "APARTMENT",
                  "reason": "SALE",
                  "rooms": 2,
                  "bathRooms": 1,
                  "area": 70
                }
                """.formatted(campoDoBairro)));
    }

    private String cadastrarPj() throws Exception {
        String email = "end" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta da spec 37",
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

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
