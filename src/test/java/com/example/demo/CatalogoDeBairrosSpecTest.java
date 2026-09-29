package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.Announcement;
import com.example.demo.model.Neighborhood;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.NeighborhoodRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 25-catalogo-de-bairros. Cada teste cita o criterio que fecha. */
class CatalogoDeBairrosSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(50000);

    /** Bairro de Joao Pessoa que o acervo de teste nunca recebe anuncio. */
    private static final String SEM_ANUNCIO = "Mussuré";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private NeighborhoodRepository neighborhoodRepository;

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

    // ---------- C1 a C3: a tabela e a carga ----------

    @Test
    @DisplayName("C1 - existe tabela de bairros por cidade")
    void c1() {
        assertThat(neighborhoodRepository.count()).isPositive();
    }

    @Test
    @DisplayName("C2 - a migracao semeia os bairros de Joao Pessoa")
    void c2() {
        List<String> bairros = neighborhoodRepository.nomesDe("joao pessoa");

        assertThat(bairros)
            .contains("Manaíra", "Bessa", "Tambaú", "Centro", SEM_ANUNCIO)
            .hasSize(64);
    }

    @Test
    @DisplayName("C3 - o mesmo bairro nao entra duas vezes na mesma cidade")
    void c3() {
        Neighborhood repetido = new Neighborhood();

        repetido.setCitySearch("João Pessoa");
        repetido.setName("manaira");

        // Nome em caixa e sem acento diferentes: a unicidade e sobre a forma
        // normalizada, senao "Manaira" e "Manaíra" viram dois bairros.
        assertThatThrownBy(() -> neighborhoodRepository.saveAndFlush(repetido))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- C4 a C7: a lista servida ----------

    @Test
    @DisplayName("C4 - a lista inclui bairro que nao tem nenhum anuncio")
    void c4() throws Exception {
        criar("Unico anuncio", "João Pessoa", "Manaíra");

        mockMvc.perform(get("/api/v1/announcements/bairros").param("city", "João Pessoa"))
            .andExpect(jsonPath("$[?(@ == '%s')]".formatted(SEM_ANUNCIO)).exists());
    }

    @Test
    @DisplayName("C5 - a rota responde no mesmo endereco e formato de antes")
    void c5() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/bairros").param("city", "João Pessoa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").isString());
    }

    /**
     * Alfabetica para quem le portugues, e nao por codigo de caractere: "Água
     * Fria" vem logo depois de "Aeroclube", nao no fim da lista.
     */
    @Test
    @DisplayName("C6 - a lista vem em ordem alfabetica")
    void c6() {
        List<String> bairros = neighborhoodRepository.nomesDe("joao pessoa");

        List<String> esperada = bairros.stream()
            .sorted(java.util.Comparator.comparing(Announcement::normalizar))
            .toList();

        assertThat(bairros).containsExactlyElementsOf(esperada);
        assertThat(bairros.indexOf("Água Fria")).isEqualTo(1);
    }

    @Test
    @DisplayName("C7 - a lista nao repete bairro")
    void c7() {
        List<String> bairros = neighborhoodRepository.nomesDe("joao pessoa");

        assertThat(bairros).doesNotHaveDuplicates();
    }

    // ---------- C8: cidade sem catalogo ----------

    @Test
    @DisplayName("C8 - cidade sem catalogo devolve os bairros que tem anuncio")
    void c8() throws Exception {
        criar("No Recife", "Recife", "Boa Viagem");
        criar("Tambem no Recife", "Recife", "Espinheiro");

        assertThat(neighborhoodRepository.nomesDe("recife")).isEmpty();

        mockMvc.perform(get("/api/v1/announcements/bairros").param("city", "Recife"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0]").value("Boa Viagem"));
    }

    // ---------- C9 e C10: filtrar por bairro do catalogo ----------

    @Test
    @DisplayName("C9 - filtrar por bairro do catalogo que tem anuncio devolve o anuncio")
    void c9() throws Exception {
        criar("No Manaira", "João Pessoa", "Manaíra");
        criar("No Bessa", "João Pessoa", "Bessa");

        mockMvc.perform(get("/api/v1/announcements").param("neighborhood", "Manaíra"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("No Manaira"));
    }

    @Test
    @DisplayName("C10 - filtrar por bairro do catalogo sem anuncio devolve pagina vazia")
    void c10() throws Exception {
        criar("No Manaira", "João Pessoa", "Manaíra");

        mockMvc.perform(get("/api/v1/announcements").param("neighborhood", SEM_ANUNCIO))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------- correcao: a lista de cidades tambem olha o catalogo ----------

    @Test
    @DisplayName("cidade com catalogo e sem anuncio aparece na lista de cidades")
    void cidadeComCatalogoSemAnuncio() throws Exception {
        criar("So no Recife", "Recife", "Boa Viagem");

        mockMvc.perform(get("/api/v1/announcements/cidades"))
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("João Pessoa")))
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("Recife")));
    }

    @Test
    @DisplayName("cidade que tem catalogo e anuncio aparece uma vez so")
    void cidadeComCatalogoEAnuncio() throws Exception {
        criar("No Manaira", "João Pessoa", "Manaíra");

        String corpo = mockMvc.perform(get("/api/v1/announcements/cidades"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(corpo.split("João Pessoa", -1)).hasSize(2);
    }

    // ---------- apoio ----------

    private static org.assertj.core.api.AbstractThrowableAssert<?, ? extends Throwable>
        assertThatThrownBy(org.assertj.core.api.ThrowableAssert.ThrowingCallable acao) {

        return org.assertj.core.api.Assertions.assertThatThrownBy(acao);
    }

    private String cadastrarPj() throws Exception {
        String email = "cat" + SEQUENCIA.getAndIncrement() + "@t.dev";

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

    private void criar(String titulo, String cidade, String bairro) throws Exception {
        mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "description": "Anuncio da suite",
                      "image": "https://images.unsplash.com/foto.jpg",
                      "price": 300000,
                      "city": "%s",
                      "state": "PB",
                      "neighborhood": "%s",
                      "street": "Rua do Sol",
                      "streetNumber": 100,
                      "cep": "58038-000",
                      "propertyType": "APARTMENT",
                      "reason": "SALE",
                      "rooms": 2,
                      "bathRooms": 1,
                      "area": 70
                    }
                    """.formatted(titulo, cidade, bairro)))
            .andExpect(status().isOk());
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
