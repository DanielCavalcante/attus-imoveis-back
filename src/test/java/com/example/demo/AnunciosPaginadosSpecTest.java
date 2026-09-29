package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
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

/** Cobre a spec 15-anuncios-paginados. Cada teste cita o criterio que fecha. */
class AnunciosPaginadosSpecTest extends PostgresIntegrationTest {

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

    // ---------- C1 a C4 ----------

    @Test
    @DisplayName("C1 - a listagem publica aceita page e size")
    void c1() throws Exception {
        mockMvc.perform(get("/api/v1/announcements").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("C2 - a resposta traz o total de elementos")
    void c2() throws Exception {
        criarAnuncios(3);

        mockMvc.perform(get("/api/v1/announcements").param("size", "2"))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("C3 - com tres anuncios e size=2, a primeira pagina traz dois")
    void c3() throws Exception {
        criarAnuncios(3);

        mockMvc.perform(get("/api/v1/announcements").param("page", "0").param("size", "2"))
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("C4 - a segunda pagina traz o restante")
    void c4() throws Exception {
        criarAnuncios(3);

        mockMvc.perform(get("/api/v1/announcements").param("page", "1").param("size", "2"))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - a listagem publica vem do mais recente para o mais antigo")
    void c5() throws Exception {
        List<Long> ordemDeCriacao = criarAnuncios(3);

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(jsonPath("$.content[0].id").value(ordemDeCriacao.get(2)))
            .andExpect(jsonPath("$.content[2].id").value(ordemDeCriacao.get(0)));
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - sem page nem size, a pagina tem tamanho 12")
    void c6() throws Exception {
        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(jsonPath("$.size").value(12));
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - size=1000 e reduzido ao teto de 50")
    void c7() throws Exception {
        mockMvc.perform(get("/api/v1/announcements").param("size", "1000"))
            .andExpect(jsonPath("$.size").value(50));
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - com dez anuncios e size=5, a pagina traz cinco")
    void c8() throws Exception {
        criarAnuncios(10);

        mockMvc.perform(get("/api/v1/announcements").param("size", "5"))
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.totalElements").value(10));
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - a listagem publica paginada segue acessivel sem autenticacao")
    void c9() throws Exception {
        criarAnuncios(2);

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - a listagem da conta pagina")
    void c10() throws Exception {
        String email = cadastrar();
        criarAnunciosDe(email, 3);

        mockMvc.perform(get("/api/v1/announcements/me")
                .param("size", "2")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---------- C11 ----------

    @Test
    @DisplayName("C11 - os favoritos paginam")
    void c11() throws Exception {
        String email = cadastrar();
        List<Long> anuncios = criarAnunciosDe(email, 3);

        for (Long anuncio : anuncios) {
            favoritar(anuncio, email);
        }

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .param("size", "2")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---------- C12 e C13 ----------

    @Test
    @DisplayName("C12 - o historico pagina")
    void c12() throws Exception {
        String email = cadastrar();
        List<Long> anuncios = criarAnunciosDe(email, 3);

        for (Long anuncio : anuncios) {
            verDetalhe(anuncio, email);
        }

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .param("size", "2")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("C13 - o historico paginado mantem o mais recente primeiro")
    void c13() throws Exception {
        String email = cadastrar();
        List<Long> anuncios = criarAnunciosDe(email, 3);

        for (Long anuncio : anuncios) {
            verDetalhe(anuncio, email);
            Thread.sleep(15);
        }

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(jsonPath("$.content[0].id").value(anuncios.get(2)))
            .andExpect(jsonPath("$.content[2].id").value(anuncios.get(0)));
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - favorito de outra conta fica fora da pagina desta")
    void c14() throws Exception {
        String vizinho = cadastrar();
        Long anuncio = criarAnunciosDe(vizinho, 1).get(0);
        favoritar(anuncio, vizinho);

        String email = cadastrar();

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---------- apoio ----------

    private void favoritar(Long anuncio, String email) throws Exception {
        mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/favorito")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isNoContent());
    }

    private void verDetalhe(Long anuncio, String email) throws Exception {
        mockMvc.perform(get("/api/v1/announcements/" + anuncio)
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());
    }

    /** Conta PJ: a quota da pessoa fisica pararia em tres anuncios. */
    private List<Long> criarAnuncios(int quantos) throws Exception {
        return criarAnunciosDe(cadastrarPj(), quantos);
    }

    private List<Long> criarAnunciosDe(String email, int quantos) throws Exception {
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < quantos; i++) {
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

            ids.add(mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong());

            // createdAt tem resolucao de microssegundo, mas o espacamento deixa
            // a ordem estavel independente de resolucao do relogio.
            Thread.sleep(15);
        }

        return ids;
    }

    private String cadastrar() throws Exception {
        String email = cadastrarComTipo("PJ", "\"%014d\"".formatted(SEQUENCIA.getAndIncrement()));

        // Toda conta desta suite anuncia, e desde a spec 28 anunciar exige
        // analise aprovada.
        aprovarEmpresa(email);

        return email;
    }

    private String cadastrarPj() throws Exception {
        return cadastrar();
    }

    private String cadastrarComTipo(String tipo, String cnpj) throws Exception {
        String email = "p" + SEQUENCIA.getAndIncrement() + "@t.dev";

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
                    """.formatted(email, String.format("11%09d", SEQUENCIA.getAndIncrement()), tipo, cnpj)))
            .andExpect(status().isCreated());

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
