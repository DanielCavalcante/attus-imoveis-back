package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.enums.UserRole;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 11-correcoes-de-seguranca. Cada teste cita o criterio que fecha. */
class CorrecoesDeSegurancaSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        announcementRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - a lista da conta sem autenticacao responde 403")
    void c1() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/me"))
            .andExpect(status().isForbidden());
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - a lista da conta com sessao responde 200")
    void c2() throws Exception {
        String email = cadastrar();

        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - a listagem publica de anuncios segue respondendo 200")
    void c3() throws Exception {
        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(status().isOk());
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - o detalhe publico de anuncio segue respondendo 200")
    void c4() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        mockMvc.perform(get("/api/v1/announcements/" + anuncio))
            .andExpect(status().isOk());
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - a quota sem autenticacao segue respondendo 403")
    void c5() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/me/quota"))
            .andExpect(status().isForbidden());
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - papel rebaixado no banco nega acesso interno com token antigo")
    void c6() throws Exception {
        String email = cadastrar();

        // Token emitido enquanto a conta era interna.
        trocarPapel(email, UserRole.INTERNO);
        String tokenDeQuandoEraInterno = tokenDe(email);

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + tokenDeQuandoEraInterno))
            .andExpect(status().isNoContent());

        trocarPapel(email, UserRole.COMUM);

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + tokenDeQuandoEraInterno))
            .andExpect(status().isForbidden());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - papel promovido no banco concede acesso com token antigo")
    void c7() throws Exception {
        String email = cadastrar();

        // Token emitido enquanto a conta era comum.
        String tokenDeQuandoEraComum = tokenDe(email);

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + tokenDeQuandoEraComum))
            .andExpect(status().isForbidden());

        trocarPapel(email, UserRole.INTERNO);

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + tokenDeQuandoEraComum))
            .andExpect(status().isNoContent());
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - pedido de apoio em anuncio alheio segue respondendo 403")
    void c8() throws Exception {
        String dono = cadastrar();
        Long anuncio = criarAnuncio(dono);

        String estranho = cadastrar();

        mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/apoio")
                .header("Authorization", "Bearer " + tokenDe(estranho)))
            .andExpect(status().isForbidden());
    }

    // ---------- apoio ----------

    private void trocarPapel(String email, UserRole papel) {
        User user = userRepository.findByEmail(email).orElseThrow();

        user.setRole(papel);

        userRepository.save(user);
    }

    private Long criarAnuncio(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Apartamento %d",
                      "description": "Anuncio da suite",
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
                    """.formatted(SEQUENCIA.getAndIncrement())))
            .andExpect(status().isOk())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String cadastrar() throws Exception {
        String email = "g" + SEQUENCIA.getAndIncrement() + "@t.dev";

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
                      "accountType": "PF"
                    }
                    """.formatted(email, String.format("11%09d", SEQUENCIA.getAndIncrement()))))
            .andExpect(status().isCreated());

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
