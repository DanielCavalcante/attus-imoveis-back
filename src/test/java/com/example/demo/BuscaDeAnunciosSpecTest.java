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
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 16-busca-e-ordenacao-no-servidor. Cada teste cita o criterio que fecha. */
class BuscaDeAnunciosSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String dono;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        userRepository.deleteAll();

        dono = cadastrarPj();
    }

    // ---------- C1 e C2 ----------

    @Test
    @DisplayName("C1 - a listagem aceita o parametro q")
    void c1() throws Exception {
        mockMvc.perform(get("/api/v1/announcements").param("q", "qualquer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("C2 - busca por trecho do titulo devolve o anuncio")
    void c2() throws Exception {
        criar("Cobertura com vista para o mar", "Recife", "PE", 500000);
        criar("Kitnet no centro", "Recife", "PE", 100000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "cobertura"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Cobertura com vista para o mar"));
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - busca por cidade devolve o anuncio")
    void c3() throws Exception {
        criar("Casa boa", "Curitiba", "PR", 300000);
        criar("Outra casa", "Recife", "PE", 300000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "curitiba"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - busca em caixa diferente devolve o anuncio")
    void c4() throws Exception {
        criar("Cobertura Duplex", "Recife", "PE", 500000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "COBERTURA duplex"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - busca sem acento acha cidade com acento")
    void c5() throws Exception {
        criar("Apartamento central", "São Paulo", "SP", 800000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "sao paulo"))
            .andExpect(jsonPath("$.totalElements").value(1));

        // E o caminho inverso: quem digita com acento tambem acha.
        mockMvc.perform(get("/api/v1/announcements").param("q", "São Paulo"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - busca sem correspondencia devolve pagina vazia")
    void c6() throws Exception {
        criar("Casa boa", "Recife", "PE", 300000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "iglu na groenlandia"))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------- C7 e C8 ----------

    @Test
    @DisplayName("C7 - sem q, devolve o acervo inteiro paginado")
    void c7() throws Exception {
        criar("Casa boa", "Recife", "PE", 300000);
        criar("Outra casa", "Curitiba", "PR", 400000);

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("C8 - q em branco devolve o acervo inteiro")
    void c8() throws Exception {
        criar("Casa boa", "Recife", "PE", 300000);
        criar("Outra casa", "Curitiba", "PR", 400000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "   "))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - a busca respeita a paginacao")
    void c9() throws Exception {
        criar("Casa na praia um", "Recife", "PE", 100000);
        criar("Casa na praia dois", "Recife", "PE", 200000);
        criar("Casa na praia tres", "Recife", "PE", 300000);

        mockMvc.perform(get("/api/v1/announcements").param("q", "praia").param("size", "2"))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---------- C10 e C11 ----------

    @Test
    @DisplayName("C10 - sort=price,desc traz o mais caro primeiro")
    void c10() throws Exception {
        criar("Barato", "Recife", "PE", 100000);
        criar("Caro", "Recife", "PE", 900000);

        mockMvc.perform(get("/api/v1/announcements").param("sort", "price,desc"))
            .andExpect(jsonPath("$.content[0].title").value("Caro"));
    }

    @Test
    @DisplayName("C11 - sort=price,asc traz o mais barato primeiro")
    void c11() throws Exception {
        criar("Barato", "Recife", "PE", 100000);
        criar("Caro", "Recife", "PE", 900000);

        mockMvc.perform(get("/api/v1/announcements").param("sort", "price,asc"))
            .andExpect(jsonPath("$.content[0].title").value("Barato"));
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - sem sort, a ordem continua do mais recente")
    void c12() throws Exception {
        criar("Primeiro", "Recife", "PE", 900000);
        Thread.sleep(15);
        criar("Segundo", "Recife", "PE", 100000);

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(jsonPath("$.content[0].title").value("Segundo"));
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - ordenacao por campo fora da lista e recusada com 400")
    void c13() throws Exception {
        mockMvc.perform(get("/api/v1/announcements").param("sort", "user.password,asc"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/announcements").param("sort", "searchText,asc"))
            .andExpect(status().isBadRequest());
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - o indice de busca existe apos a migracao")
    void c14() {
        Integer total = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_announcements_busca'",
            Integer.class
        );

        assertThat(total).isEqualTo(1);
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - busca combina com ordenacao")
    void c15() throws Exception {
        criar("Casa na praia cara", "Recife", "PE", 900000);
        criar("Casa na praia barata", "Recife", "PE", 100000);
        criar("Apartamento no centro", "Recife", "PE", 50000);

        mockMvc.perform(get("/api/v1/announcements")
                .param("q", "praia")
                .param("sort", "price,asc"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].title").value("Casa na praia barata"))
            .andExpect(jsonPath("$.content[1].title").value("Casa na praia cara"));
    }

    // ---------- apoio ----------

    private void criar(String titulo, String cidade, String uf, int preco) throws Exception {
        mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "description": "Anuncio da suite",
                      "image": "https://images.unsplash.com/foto.jpg",
                      "price": %d,
                      "city": "%s",
                      "state": "%s",
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
                    """.formatted(titulo, preco, cidade, uf)))
            .andExpect(status().isOk());
    }

    private String cadastrarPj() throws Exception {
        String email = "s" + SEQUENCIA.getAndIncrement() + "@t.dev";

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

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
