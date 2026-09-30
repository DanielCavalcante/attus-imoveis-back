package com.example.demo;

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
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 18-filtros-do-acervo. Cada teste cita o criterio que fecha. */
class FiltrosDoAcervoSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(9000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private JwtService jwtService;

    private String dono;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        userRepository.deleteAll();

        dono = cadastrarPj();
        liberarParaAnunciar(dono);
    }

    // ---------- C1 a C3: cidade ----------

    @Test
    @DisplayName("C1 - city devolve apenas anuncios daquela cidade")
    void c1() throws Exception {
        criar("Apartamento no Recife", "Recife", "PE", 500_000, "APARTMENT", "SALE");
        criar("Casa em Olinda", "Olinda", "PE", 400_000, "HOUSE", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("city", "Recife"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].city").value("Recife"));
    }

    @Test
    @DisplayName("C2 - city sem acento encontra a cidade acentuada")
    void c2() throws Exception {
        criar("Casa no litoral", "João Pessoa", "PB", 600_000, "HOUSE", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("city", "joao pessoa"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].city").value("João Pessoa"));
    }

    @Test
    @DisplayName("C3 - city em caixa diferente encontra o anuncio")
    void c3() throws Exception {
        criar("Apartamento no Recife", "Recife", "PE", 500_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("city", "RECIFE"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C4 a C6: preco ----------

    @Test
    @DisplayName("C4 - minPrice devolve apenas preco maior ou igual")
    void c4() throws Exception {
        criar("Barato", "Recife", "PE", 100_000, "APARTMENT", "SALE");
        criar("Caro", "Recife", "PE", 900_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("minPrice", "500000"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Caro"));
    }

    @Test
    @DisplayName("C5 - maxPrice devolve apenas preco menor ou igual")
    void c5() throws Exception {
        criar("Barato", "Recife", "PE", 100_000, "APARTMENT", "SALE");
        criar("Caro", "Recife", "PE", 900_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("maxPrice", "500000"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Barato"));
    }

    @Test
    @DisplayName("C6 - minPrice e maxPrice juntos devolvem so a faixa fechada")
    void c6() throws Exception {
        criar("Abaixo", "Recife", "PE", 100_000, "APARTMENT", "SALE");
        criar("Dentro", "Recife", "PE", 500_000, "APARTMENT", "SALE");
        criar("Acima", "Recife", "PE", 900_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements")
                .param("minPrice", "400000")
                .param("maxPrice", "600000"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Dentro"));
    }

    // ---------- C7 e C8: tipo ----------

    @Test
    @DisplayName("C7 - type=APARTMENT devolve apenas apartamentos")
    void c7() throws Exception {
        criar("Apto", "Recife", "PE", 300_000, "APARTMENT", "SALE");
        criar("Casa", "Recife", "PE", 300_000, "HOUSE", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("type", "APARTMENT"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].propertyType").value("APARTMENT"));
    }

    @Test
    @DisplayName("C8 - type repetido devolve a uniao dos tipos pedidos")
    void c8() throws Exception {
        criar("Casa", "Recife", "PE", 300_000, "HOUSE", "SALE");
        criar("Terreno", "Recife", "PE", 300_000, "LAND", "SALE");
        criar("Apto", "Recife", "PE", 300_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements")
                .param("type", "HOUSE")
                .param("type", "LAND"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[?(@.propertyType == 'APARTMENT')]").isEmpty());
    }

    // ---------- C9: finalidade ----------

    @Test
    @DisplayName("C9 - reason=RENT devolve apenas aluguel")
    void c9() throws Exception {
        criar("Para alugar", "Recife", "PE", 2_000, "APARTMENT", "RENT");
        criar("Para vender", "Recife", "PE", 500_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements").param("reason", "RENT"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].reason").value("RENT"));
    }

    // ---------- C10 e C11: combinacao ----------

    @Test
    @DisplayName("C10 - q combinado com city devolve so o que satisfaz os dois")
    void c10() throws Exception {
        criar("Cobertura com vista", "Recife", "PE", 800_000, "APARTMENT", "SALE");
        criar("Cobertura com vista", "Olinda", "PE", 800_000, "APARTMENT", "SALE");
        criar("Kitnet simples", "Recife", "PE", 150_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements")
                .param("q", "cobertura")
                .param("city", "Recife"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].city").value("Recife"));
    }

    @Test
    @DisplayName("C11 - type combinado com maxPrice devolve so o que satisfaz os dois")
    void c11() throws Exception {
        criar("Casa barata", "Recife", "PE", 200_000, "HOUSE", "SALE");
        criar("Casa cara", "Recife", "PE", 900_000, "HOUSE", "SALE");
        criar("Apto barato", "Recife", "PE", 200_000, "APARTMENT", "SALE");

        mockMvc.perform(get("/api/v1/announcements")
                .param("type", "HOUSE")
                .param("maxPrice", "500000"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Casa barata"));
    }

    // ---------- C12 e C13: ausencia e vazio ----------

    @Test
    @DisplayName("C12 - sem nenhum filtro devolve o acervo inteiro paginado")
    void c12() throws Exception {
        criar("Um", "Recife", "PE", 100_000, "APARTMENT", "SALE");
        criar("Dois", "Olinda", "PE", 200_000, "HOUSE", "RENT");
        criar("Tres", "João Pessoa", "PB", 300_000, "LAND", "SALE");

        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("C13 - combinacao sem correspondencia devolve pagina vazia e total zero")
    void c13() throws Exception {
        criar("Casa cara", "Recife", "PE", 900_000, "HOUSE", "SALE");

        mockMvc.perform(get("/api/v1/announcements")
                .param("city", "Recife")
                .param("maxPrice", "100000"))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------- C14: valor invalido ----------

    @Test
    @DisplayName("C14 - type fora do enum e recusado com 400")
    void c14() throws Exception {
        mockMvc.perform(get("/api/v1/announcements").param("type", "MANSAO"))
            .andExpect(status().isBadRequest());
    }

    // ---------- C15: filtro com ordenacao ----------

    @Test
    @DisplayName("C15 - type com sort=price,asc traz o mais barato do tipo primeiro")
    void c15() throws Exception {
        criar("Apto caro", "Recife", "PE", 900_000, "APARTMENT", "SALE");
        criar("Apto barato", "Recife", "PE", 200_000, "APARTMENT", "SALE");
        criar("Casa baratissima", "Recife", "PE", 50_000, "HOUSE", "SALE");

        mockMvc.perform(get("/api/v1/announcements")
                .param("type", "APARTMENT")
                .param("sort", "price,asc"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].title").value("Apto barato"));
    }

    // ---------- apoio ----------

    private String cadastrarPj() throws Exception {
        String email = "f" + SEQUENCIA.getAndIncrement() + "@t.dev";

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

    private void criar(
        String titulo,
        String cidade,
        String uf,
        int preco,
        String tipo,
        String finalidade
    ) throws Exception {
        mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "description": "Anuncio da suite",
                      "image": "/api/v1/imagens/foto-de-teste.png",
                      "price": %d,
                      "city": "%s",
                      "state": "%s",
                      "street": "Rua do Sol",
                      "streetNumber": 100,
                      "cep": "50000-000",
                      "complemento": "",
                      "propertyType": "%s",
                      "reason": "%s",
                      "rooms": 2,
                      "bathRooms": 1,
                      "area": 70
                    }
                    """.formatted(titulo, preco, cidade, uf, tipo, finalidade)))
            .andExpect(status().isOk());
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
