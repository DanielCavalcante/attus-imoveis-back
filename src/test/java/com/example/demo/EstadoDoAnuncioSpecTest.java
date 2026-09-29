package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 22-anuncio-ativo-e-quota-por-ativos. Cada teste cita o criterio. */
class EstadoDoAnuncioSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(30000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper json = new ObjectMapper();

    private String dono;
    private String intruso;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        userRepository.deleteAll();

        dono = cadastrar("PJ");
        intruso = cadastrar("PJ");
    }

    // ---------- C1: nasce ativo ----------

    @Test
    @DisplayName("C1 - anuncio recem-criado nasce ativo")
    void c1() throws Exception {
        criar(dono, "Novo");

        assertThat(announcementRepository.findAll().get(0).isActive()).isTrue();
    }

    // ---------- C2 a C4: as rotas existem ----------

    @Test
    @DisplayName("C2 - existe rota para inativar anuncio da propria conta")
    void c2() throws Exception {
        long id = criar(dono, "Para inativar");

        mockMvc.perform(patch("/api/v1/announcements/{id}/inativar", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());

        assertThat(announcementRepository.findById(id).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("C3 - existe rota para reativar anuncio da propria conta")
    void c3() throws Exception {
        long id = criar(dono, "Para reativar");

        inativar(dono, id);

        mockMvc.perform(patch("/api/v1/announcements/{id}/reativar", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());

        assertThat(announcementRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("C4 - existe rota para excluir anuncio da propria conta")
    void c4() throws Exception {
        long id = criar(dono, "Para excluir");

        mockMvc.perform(delete("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());

        assertThat(announcementRepository.findById(id)).isEmpty();
    }

    // ---------- C5 e C6: dono verificado ----------

    @Test
    @DisplayName("C5 - inativar anuncio de outra conta e recusado")
    void c5() throws Exception {
        long id = criar(dono, "Do dono");

        mockMvc.perform(patch("/api/v1/announcements/{id}/inativar", id)
                .header("Authorization", "Bearer " + tokenDe(intruso)))
            .andExpect(status().isNotFound());

        assertThat(announcementRepository.findById(id).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("C6 - excluir anuncio de outra conta e recusado")
    void c6() throws Exception {
        long id = criar(dono, "Do dono");

        mockMvc.perform(delete("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(intruso)))
            .andExpect(status().isNotFound());

        assertThat(announcementRepository.findById(id)).isPresent();
    }

    // ---------- C7 a C9: visibilidade ----------

    @Test
    @DisplayName("C7 - anuncio inativo nao aparece na listagem publica")
    void c7() throws Exception {
        long id = criar(dono, "Some daqui");
        criar(dono, "Fica aqui");

        inativar(dono, id);

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Fica aqui"));
    }

    @Test
    @DisplayName("C8 - detalhe de anuncio inativo nao e servido a quem nao e o dono")
    void c8() throws Exception {
        long id = criar(dono, "Fora do ar");

        inativar(dono, id);

        mockMvc.perform(get("/api/v1/announcements/{id}", id))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C9 - anuncio inativo continua na listagem da propria conta")
    void c9() throws Exception {
        long id = criar(dono, "Fora do ar");

        inativar(dono, id);

        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Fora do ar"));
    }

    // ---------- C10 a C12: o saldo ----------

    @Test
    @DisplayName("C10 - o saldo conta anuncios ativos, nao criados no mes")
    void c10() throws Exception {
        String pf = cadastrar("PF");

        long primeiro = criar(pf, "Um");
        criar(pf, "Dois");

        assertThat(usados(pf)).isEqualTo(2);

        inativar(pf, primeiro);

        // Criado no mes continua sendo 2; no ar passa a ser 1.
        assertThat(usados(pf)).isEqualTo(1);
    }

    @Test
    @DisplayName("C11 - inativar devolve uma vaga ao saldo, em qualquer tipo de conta")
    void c11() throws Exception {
        long id = criar(dono, "Da PJ");

        assertThat(usados(dono)).isEqualTo(1);

        inativar(dono, id);

        assertThat(usados(dono)).isZero();
    }

    @Test
    @DisplayName("C12 - excluir devolve uma vaga ao saldo, em qualquer tipo de conta")
    void c12() throws Exception {
        long id = criar(dono, "Da PJ");

        assertThat(usados(dono)).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());

        assertThat(usados(dono)).isZero();
    }

    // ---------- C13 e C14: o teto ----------

    @Test
    @DisplayName("C13 - reativar com a quota cheia e recusado com 409")
    void c13() throws Exception {
        String pf = cadastrar("PF");

        long primeiro = criar(pf, "Um");
        criar(pf, "Dois");
        criar(pf, "Tres");

        inativar(pf, primeiro);

        // A vaga liberada e ocupada por um novo: reativar o antigo estoura.
        criar(pf, "Quatro");

        mockMvc.perform(patch("/api/v1/announcements/{id}/reativar", primeiro)
                .header("Authorization", "Bearer " + tokenDe(pf)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C14 - criar com a quota cheia continua recusado com 409")
    void c14() throws Exception {
        String pf = cadastrar("PF");

        criar(pf, "Um");
        criar(pf, "Dois");
        criar(pf, "Tres");

        mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(pf))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("Quatro")))
            .andExpect(status().isConflict());
    }

    // ---------- C15: a migracao ----------

    @Test
    @DisplayName("C15 - os anuncios ja publicados ficam ativos depois da migracao")
    void c15() throws Exception {
        criar(dono, "Publicado antes");

        // A coluna nasce NOT NULL DEFAULT TRUE e a carga deixa ativo o que
        // existe: nenhum anuncio sai do ar por causa da V6.
        assertThat(announcementRepository.findAll())
            .isNotEmpty()
            .allSatisfy(a -> assertThat(a.isActive()).isTrue());
    }

    // ---------- apoio ----------

    private long usados(String email) throws Exception {
        String corpo = mockMvc.perform(get("/api/v1/announcements/me/quota")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode no = json.readTree(corpo);

        return no.get("usados").asLong();
    }

    private void inativar(String email, long id) throws Exception {
        mockMvc.perform(patch("/api/v1/announcements/{id}/inativar", id)
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isNoContent());
    }

    private String cadastrar(String tipo) throws Exception {
        String email = "e" + SEQUENCIA.getAndIncrement() + "@t.dev";

        String cnpj = "PJ".equals(tipo)
            ? "\"cnpj\": \"%014d\",".formatted(SEQUENCIA.getAndIncrement())
            : "";

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
                      %s
                      "extra": null
                    }
                    """.formatted(
                        email,
                        String.format("11%09d", SEQUENCIA.getAndIncrement()),
                        tipo,
                        cnpj)))
            .andExpect(status().isCreated());

        return email;
    }

    private long criar(String email, String titulo) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(titulo)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return json.readTree(corpo).get("id").asLong();
    }

    private String payload(String titulo) {
        return """
            {
              "title": "%s",
              "description": "Anuncio da suite",
              "image": "https://images.unsplash.com/foto.jpg",
              "price": 300000,
              "city": "João Pessoa",
              "state": "PB",
              "street": "Rua do Sol",
              "streetNumber": 100,
              "cep": "58038-000",
              "propertyType": "APARTMENT",
              "reason": "SALE",
              "rooms": 2,
              "bathRooms": 1,
              "area": 70
            }
            """.formatted(titulo);
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
