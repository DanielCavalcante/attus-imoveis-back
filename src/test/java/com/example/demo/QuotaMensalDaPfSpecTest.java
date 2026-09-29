package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 06-quota-mensal-da-pf. Cada teste cita o criterio que fecha. */
class QuotaMensalDaPfSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpar() {
        announcementRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - PF sem anuncio no mes cria o primeiro")
    void c1() throws Exception {
        String email = cadastrar("PF");

        criarAnuncio(email).andExpect(status().isOk());
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - PF com dois anuncios no mes cria o terceiro")
    void c2() throws Exception {
        String email = cadastrar("PF");

        criarAnuncio(email).andExpect(status().isOk());
        criarAnuncio(email).andExpect(status().isOk());
        criarAnuncio(email).andExpect(status().isOk());
    }

    // ---------- C3 e C4 ----------

    @Test
    @DisplayName("C3 - PF com tres anuncios no mes e recusada no quarto")
    void c3() throws Exception {
        String email = cadastrar("PF");

        for (int i = 0; i < 3; i++) {
            criarAnuncio(email).andExpect(status().isOk());
        }

        criarAnuncio(email).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("C4 - a recusa por quota responde 409")
    void c4() throws Exception {
        String email = cadastrar("PF");

        for (int i = 0; i < 3; i++) {
            criarAnuncio(email).andExpect(status().isOk());
        }

        criarAnuncio(email).andExpect(status().isConflict());
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - anuncio recusado por quota nao e gravado")
    void c5() throws Exception {
        String email = cadastrar("PF");

        for (int i = 0; i < 3; i++) {
            criarAnuncio(email).andExpect(status().isOk());
        }

        criarAnuncio(email).andExpect(status().isConflict());

        assertThat(announcementRepository.findByUserEmail(email)).hasSize(3);
    }

    // ---------- C6 ----------

    /**
     * C6 foi substituido pela spec 22, e este teste guarda a substituicao.
     *
     * Enquanto a quota contava criacoes do mes, envelhecer os anuncios abria
     * vaga. Contando anuncios no ar, idade deixou de importar: o que devolve
     * vaga e inativar ou excluir. Manter o teste antigo verde exigiria manter
     * as duas regras ao mesmo tempo, que e o que a spec 22 recusa.
     */
    @Test
    @DisplayName("C6 (substituido pela spec 22) - idade nao abre vaga; inativar abre")
    void c6() throws Exception {
        String email = cadastrar("PF");

        for (int i = 0; i < 3; i++) {
            criarAnuncio(email).andExpect(status().isOk());
        }

        envelhecerAnunciosDe(email);

        criarAnuncio(email).andExpect(status().isConflict());

        Long id = announcementRepository.findByUserEmail(email).get(0).getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/announcements/{id}/inativar", id)
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isNoContent());

        criarAnuncio(email).andExpect(status().isOk());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - anuncio de outra conta nao conta para a quota desta")
    void c7() throws Exception {
        String vizinho = cadastrar("PF");

        for (int i = 0; i < 3; i++) {
            criarAnuncio(vizinho).andExpect(status().isOk());
        }

        String email = cadastrar("PF");

        criarAnuncio(email).andExpect(status().isOk());
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - PJ com tres anuncios no mes cria o quarto")
    void c8() throws Exception {
        String email = cadastrar("PJ");

        for (int i = 0; i < 4; i++) {
            criarAnuncio(email).andExpect(status().isOk());
        }
    }

    // ---------- C10, C11, C12 ----------

    @Test
    @DisplayName("C10 - a rota de quota devolve o limite vigente")
    void c10() throws Exception {
        String email = cadastrar("PF");

        consultarQuota(email).andExpect(jsonPath("$.limite").value(3));
    }

    @Test
    @DisplayName("C11 - a rota de quota devolve quantos foram usados no mes")
    void c11() throws Exception {
        String email = cadastrar("PF");

        criarAnuncio(email).andExpect(status().isOk());
        criarAnuncio(email).andExpect(status().isOk());

        consultarQuota(email).andExpect(jsonPath("$.usados").value(2));
    }

    @Test
    @DisplayName("C12 - a rota de quota devolve quantos restam")
    void c12() throws Exception {
        String email = cadastrar("PF");

        criarAnuncio(email).andExpect(status().isOk());

        consultarQuota(email).andExpect(jsonPath("$.restantes").value(2));
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - a rota de quota indica que PJ nao tem limite")
    void c13() throws Exception {
        String email = cadastrar("PJ");

        consultarQuota(email).andExpect(jsonPath("$.ilimitado").value(true));
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - a rota de quota sem autenticacao responde 403")
    void c14() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/me/quota"))
            .andExpect(status().isForbidden());
    }

    // ---------- apoio ----------

    private org.springframework.test.web.servlet.ResultActions consultarQuota(String email)
        throws Exception {

        return mockMvc.perform(get("/api/v1/announcements/me/quota")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions criarAnuncio(String email)
        throws Exception {

        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Apartamento de teste %d",
                  "description": "Anuncio criado pela suite",
                  "image": "https://images.unsplash.com/foto.jpg",
                  "price": 450000,
                  "city": "Recife",
                  "state": "PE",
                  "street": "Rua do Sol",
                  "streetNumber": 100,
                  "cep": "50000-000",
                  "complemento": "",
                  "propertyType": "APARTMENT",
                  "reason": "SALE",
                  "rooms": 2,
                  "bathRooms": 1,
                  "area": 70
                }
                """.formatted(SEQUENCIA.getAndIncrement())));
    }

    /** Empurra os anuncios da conta para o mes passado, sem depender do relogio. */
    private void envelhecerAnunciosDe(String email) {
        jdbcTemplate.update(
            "UPDATE announcements SET created_at = created_at - INTERVAL '40 days' "
                + "WHERE user_id = (SELECT id FROM users WHERE email = ?)",
            email
        );
    }

    private String cadastrar(String tipo) throws Exception {
        String email = emailUnico();
        String cnpj = "PJ".equals(tipo) ? "\"%s\"".formatted(cnpjUnico()) : "null";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta de teste",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "%s",
                      "cnpj": %s
                    }
                    """.formatted(email, telefoneUnico(), tipo, cnpj)))
            .andExpect(status().isCreated());

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }

    private String emailUnico() {
        return "q" + SEQUENCIA.getAndIncrement() + "@t.dev";
    }

    private String telefoneUnico() {
        return String.format("11%09d", SEQUENCIA.getAndIncrement());
    }

    private String cnpjUnico() {
        return String.format("%014d", SEQUENCIA.getAndIncrement());
    }
}
