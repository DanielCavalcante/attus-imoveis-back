package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 24-coerencia-do-anuncio-fora-do-ar. Cada teste cita o criterio. */
class CoerenciaDoAnuncioForaDoArSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(40000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper json = new ObjectMapper();

    /** Quem anuncia. */
    private String dono;

    /** Quem navega: favorita e visita o anuncio dos outros. */
    private String visitante;

    @BeforeEach
    void preparar() throws Exception {
        favoriteRepository.deleteAll();
        announcementRepository.deleteAll();
        userRepository.deleteAll();

        dono = cadastrar("PJ");
        visitante = cadastrar("PF");

        liberarParaAnunciar(dono);
    }

    // ---------- C1 a C3: favoritos ----------

    @Test
    @DisplayName("C1 - anuncio inativado nao aparece nos favoritos de quem o favoritou")
    void c1() throws Exception {
        long some = criar("Vai sair do ar");
        long fica = criar("Continua no ar");

        favoritar(some);
        favoritar(fica);

        inativar(some);

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.content[?(@.title == 'Vai sair do ar')]").isEmpty())
            .andExpect(jsonPath("$.content[0].title").value("Continua no ar"));
    }

    @Test
    @DisplayName("C2 - o favorito nao e apagado: reativado, o anuncio volta a lista")
    void c2() throws Exception {
        long id = criar("Vai e volta");

        favoritar(id);
        inativar(id);

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.totalElements").value(0));

        reativar(id);

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Vai e volta"));
    }

    @Test
    @DisplayName("C3 - o total da pagina de favoritos nao conta anuncio fora do ar")
    void c3() throws Exception {
        long some = criar("Fora do ar");
        long fica = criar("No ar");

        favoritar(some);
        favoritar(fica);

        inativar(some);

        mockMvc.perform(get("/api/v1/announcements/me/favoritos")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C4 a C6: historico ----------

    @Test
    @DisplayName("C4 - anuncio inativado nao aparece no historico de visitas")
    void c4() throws Exception {
        long some = criar("Visitado e retirado");
        long fica = criar("Visitado e no ar");

        visitar(some);
        visitar(fica);

        inativar(some);

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.content[?(@.title == 'Visitado e retirado')]").isEmpty());
    }

    @Test
    @DisplayName("C5 - reativado, o anuncio volta ao historico")
    void c5() throws Exception {
        long id = criar("Visitado, vai e volta");

        visitar(id);
        inativar(id);

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.totalElements").value(0));

        reativar(id);

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("C6 - o total do historico nao conta anuncio fora do ar")
    void c6() throws Exception {
        long some = criar("Fora do ar");
        long fica = criar("No ar");

        visitar(some);
        visitar(fica);

        inativar(some);

        mockMvc.perform(get("/api/v1/announcements/me/historico")
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C7: favoritar o que esta fora do ar ----------

    @Test
    @DisplayName("C7 - favoritar anuncio fora do ar e recusado")
    void c7() throws Exception {
        long id = criar("Fora do ar");

        inativar(id);

        mockMvc.perform(post("/api/v1/announcements/{id}/favorito", id)
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(status().isNotFound());
    }

    // ---------- C8 a C10: editar exige ser dono ----------

    @Test
    @DisplayName("C8 - editar anuncio de outra conta e recusado")
    void c8() throws Exception {
        long id = criar("Do dono");

        mockMvc.perform(put("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(visitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("Titulo do invasor")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/announcements/{id}", id))
            .andExpect(jsonPath("$.title").value("Do dono"));
    }

    @Test
    @DisplayName("C9 - a recusa da edicao nao revela que o anuncio existe")
    void c9() throws Exception {
        long existente = criar("Existe e nao e seu");

        // Mesma resposta para o que existe e para o que nao existe: comparar os
        // dois nao conta ao curioso qual id esta ocupado.
        int deExistente = mockMvc.perform(put("/api/v1/announcements/{id}", existente)
                .header("Authorization", "Bearer " + tokenDe(visitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("x")))
            .andReturn().getResponse().getStatus();

        int deInexistente = mockMvc.perform(put("/api/v1/announcements/{id}", 987654321L)
                .header("Authorization", "Bearer " + tokenDe(visitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("x")))
            .andReturn().getResponse().getStatus();

        org.assertj.core.api.Assertions.assertThat(deExistente).isEqualTo(deInexistente);
        org.assertj.core.api.Assertions.assertThat(deExistente).isEqualTo(404);
    }

    @Test
    @DisplayName("C10 - editar o proprio anuncio continua funcionando")
    void c10() throws Exception {
        long id = criar("Titulo antigo");

        mockMvc.perform(put("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("Titulo novo")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Titulo novo"));
    }

    // ---------- apoio ----------

    private void favoritar(long id) throws Exception {
        mockMvc.perform(post("/api/v1/announcements/{id}/favorito", id)
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(status().is2xxSuccessful());
    }

    /** Visitar e abrir o detalhe: e assim que o historico e alimentado. */
    private void visitar(long id) throws Exception {
        mockMvc.perform(get("/api/v1/announcements/{id}", id)
                .header("Authorization", "Bearer " + tokenDe(visitante)))
            .andExpect(status().isOk());
    }

    private void inativar(long id) throws Exception {
        mockMvc.perform(patch("/api/v1/announcements/{id}/inativar", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());
    }

    private void reativar(long id) throws Exception {
        mockMvc.perform(patch("/api/v1/announcements/{id}/reativar", id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());
    }

    private String cadastrar(String tipo) throws Exception {
        String email = "c" + SEQUENCIA.getAndIncrement() + "@t.dev";

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

    private long criar(String titulo) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
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
              "image": "/api/v1/imagens/foto-de-teste.png",
              "price": 300000,
              "city": "Recife",
              "state": "PE",
              "street": "Rua do Sol",
              "streetNumber": 100,
              "cep": "50000-000",
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
