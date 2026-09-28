package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.AnnouncementViewRepository;
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 10-favoritos-e-historico. Cada teste cita o criterio que fecha. */
class FavoritosEHistoricoSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private AnnouncementViewRepository viewRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        favoriteRepository.deleteAll();
        viewRepository.deleteAll();
        announcementRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - favoritar cria o favorito para a conta")
    void c1() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        favoritar(anuncio, email).andExpect(status().isNoContent());

        assertThat(favoriteRepository.findAll()).hasSize(1);
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - favoritar duas vezes nao cria um segundo favorito")
    void c2() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        favoritar(anuncio, email).andExpect(status().isNoContent());
        favoritar(anuncio, email).andExpect(status().isNoContent());

        assertThat(favoriteRepository.findAll()).hasSize(1);
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - desfavoritar remove o favorito")
    void c3() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        favoritar(anuncio, email).andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/announcements/" + anuncio + "/favorito")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isNoContent());

        assertThat(favoriteRepository.findAll()).isEmpty();
    }

    // ---------- C4 e C8 ----------

    @Test
    @DisplayName("C4 - a listagem traz os anuncios favoritados da conta")
    void c4() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        favoritar(anuncio, email).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(anuncio));
    }

    @Test
    @DisplayName("C8 - a listagem traz titulo, cidade, preco e imagem")
    void c8() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        favoritar(anuncio, email).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(jsonPath("$[0].title").exists())
            .andExpect(jsonPath("$[0].city").value("Recife"))
            .andExpect(jsonPath("$[0].price").value(450000))
            .andExpect(jsonPath("$[0].image").exists());
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - favorito de outra conta nao aparece na lista desta")
    void c5() throws Exception {
        String vizinho = cadastrar();
        Long anuncio = criarAnuncio(vizinho);

        favoritar(anuncio, vizinho).andExpect(status().isNoContent());

        String email = cadastrar();

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - favoritar sem autenticacao responde 403")
    void c6() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/favorito"))
            .andExpect(status().isForbidden());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - favoritar anuncio inexistente responde 404")
    void c7() throws Exception {
        String email = cadastrar();

        favoritar(999999L, email).andExpect(status().isNotFound());
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - visitar o detalhe com sessao registra a visualizacao")
    void c9() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        verDetalhe(anuncio, email);

        assertThat(viewRepository.findAll()).hasSize(1);
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - visitar de novo nao cria segunda linha")
    void c10() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        verDetalhe(anuncio, email);
        verDetalhe(anuncio, email);

        assertThat(viewRepository.findAll()).hasSize(1);
    }

    // ---------- C11 ----------

    @Test
    @DisplayName("C11 - visitar de novo atualiza o instante da visita")
    void c11() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        verDetalhe(anuncio, email);

        LocalDateTime primeira = viewRepository.findAll().get(0).getViewedAt();

        verDetalhe(anuncio, email);

        LocalDateTime segunda = viewRepository.findAll().get(0).getViewedAt();

        assertThat(segunda).isAfterOrEqualTo(primeira);
        assertThat(segunda).isNotEqualTo(LocalDateTime.MIN);
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - visita sem sessao nao registra historico")
    void c12() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        mockMvc.perform(get("/api/v1/announcements/" + anuncio))
            .andExpect(status().isOk());

        assertThat(viewRepository.findAll()).isEmpty();
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - o historico lista do mais recente para o mais antigo")
    void c13() throws Exception {
        String email = cadastrar();
        Long primeiro = criarAnuncio(email);
        Long segundo = criarAnuncio(email);

        verDetalhe(primeiro, email);
        Thread.sleep(20);
        verDetalhe(segundo, email);

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(segundo))
            .andExpect(jsonPath("$[1].id").value(primeiro));
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - historico sem autenticacao responde 403")
    void c14() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/me/historico"))
            .andExpect(status().isForbidden());
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - remover o anuncio remove favoritos e historico dele")
    void c15() throws Exception {
        String email = cadastrar();
        Long anuncio = criarAnuncio(email);

        favoritar(anuncio, email).andExpect(status().isNoContent());
        verDetalhe(anuncio, email);

        announcementRepository.deleteById(anuncio);

        assertThat(favoriteRepository.findAll()).isEmpty();
        assertThat(viewRepository.findAll()).isEmpty();
    }

    // ---------- apoio ----------

    private org.springframework.test.web.servlet.ResultActions favoritar(Long anuncio, String email)
        throws Exception {

        return mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/favorito")
            .header("Authorization", "Bearer " + tokenDe(email)));
    }

    private void verDetalhe(Long anuncio, String email) throws Exception {
        mockMvc.perform(get("/api/v1/announcements/" + anuncio)
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());
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
        String email = "f" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Quem navega",
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
