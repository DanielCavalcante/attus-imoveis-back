package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.model.Plan;
import com.example.demo.model.User;
import com.example.demo.repositories.PlanRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 07-planos-configuraveis. Cada teste cita o criterio que fecha. */
class PlanosConfiguraveisSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        planRepository.deleteAll();
        // deleteAll carrega as entidades, entao o cascade limpa anuncios e pedidos.
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - criar plano com papel comum responde 403")
    void c1() throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenComum())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("Basico", "99.90", "10")))
            .andExpect(status().isForbidden());
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - criar plano com papel INTERNO responde 201")
    void c2() throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("Basico", "99.90", "10")))
            .andExpect(status().isCreated());
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - o plano criado fica gravado com nome, preco e limite")
    void c3() throws Exception {
        criarPlano("Intermediario", "199.90", "50");

        Plan plan = planRepository.findAll().get(0);

        assertThat(plan.getNome()).isEqualTo("Intermediario");
        assertThat(plan.getPrecoMensal()).isEqualByComparingTo(new BigDecimal("199.90"));
        assertThat(plan.getLimiteAnuncios()).isEqualTo(50);
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - criar plano sem nome responde 400 nomeando o campo")
    void c4() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("", "99.90", "10")))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("nome");
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - criar plano com preco negativo responde 400")
    void c5() throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("Negativo", "-1.00", "10")))
            .andExpect(status().isBadRequest());
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - nome de plano ja existente responde 409")
    void c6() throws Exception {
        criarPlano("Basico", "99.90", "10");

        mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("Basico", "149.90", "20")))
            .andExpect(status().isConflict());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - plano criado nasce ativo")
    void c7() throws Exception {
        criarPlano("Basico", "99.90", "10");

        assertThat(planRepository.findAll().get(0).isAtivo()).isTrue();
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - alterar plano grava o novo preco mensal")
    void c8() throws Exception {
        Long id = criarPlano("Basico", "99.90", "10");

        mockMvc.perform(put("/api/v1/backoffice/plans/" + id)
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("Basico", "129.90", "10")))
            .andExpect(status().isOk());

        assertThat(planRepository.findById(id).orElseThrow().getPrecoMensal())
            .isEqualByComparingTo(new BigDecimal("129.90"));
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - alterar plano com papel comum responde 403")
    void c9() throws Exception {
        Long id = criarPlano("Basico", "99.90", "10");

        mockMvc.perform(put("/api/v1/backoffice/plans/" + id)
                .header("Authorization", "Bearer " + tokenComum())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano("Basico", "1.00", "10")))
            .andExpect(status().isForbidden());
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - desativar deixa o plano inativo")
    void c10() throws Exception {
        Long id = criarPlano("Basico", "99.90", "10");

        desativar(id);

        assertThat(planRepository.findById(id).orElseThrow().isAtivo()).isFalse();
    }

    // ---------- C11 ----------

    @Test
    @DisplayName("C11 - plano desativado continua existindo no banco")
    void c11() throws Exception {
        Long id = criarPlano("Basico", "99.90", "10");

        desativar(id);

        assertThat(planRepository.findById(id)).isPresent();
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - o catalogo interno lista tambem os inativos")
    void c12() throws Exception {
        Long id = criarPlano("Basico", "99.90", "10");
        desativar(id);

        mockMvc.perform(get("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ativo").value(false));
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - o catalogo publico nao lista plano inativo")
    void c13() throws Exception {
        Long id = criarPlano("Basico", "99.90", "10");
        desativar(id);

        mockMvc.perform(get("/api/v1/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - o catalogo publico responde 200 sem autenticacao")
    void c14() throws Exception {
        criarPlano("Basico", "99.90", "10");

        mockMvc.perform(get("/api/v1/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Basico"));
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - plano com limite nulo e devolvido como ilimitado")
    void c15() throws Exception {
        criarPlano("Avancado", "499.90", null);

        mockMvc.perform(get("/api/v1/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].anunciosIlimitados").value(true));
    }

    // ---------- apoio ----------

    private Long criarPlano(String nome, String preco, String limite) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPlano(nome, preco, limite)))
            .andExpect(status().isCreated())
            .andReturn();

        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private void desativar(Long id) throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/plans/" + id + "/desativar")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk());
    }

    private String corpoPlano(String nome, String preco, String limite) {
        return """
            {
              "nome": "%s",
              "descricao": "Pacote de teste",
              "precoMensal": %s,
              "limiteAnuncios": %s
            }
            """.formatted(nome, preco, limite == null ? "null" : limite);
    }

    private String tokenInterno() {
        return tokenDe(conta(AccountType.INTERNO, UserRole.INTERNO));
    }

    private String tokenComum() {
        return tokenDe(conta(AccountType.PF, UserRole.COMUM));
    }

    private String conta(AccountType tipo, UserRole papel) {
        User user = new User();

        user.setFullname("Conta de teste");
        user.setEmail("p" + SEQUENCIA.getAndIncrement() + "@t.dev");
        user.setPhone(String.format("11%09d", SEQUENCIA.getAndIncrement()));
        user.setPassword(passwordEncoder.encode("Senha123"));
        user.setAccountType(tipo);
        user.setAccountStatus(AccountStatus.ATIVO);
        user.setRole(papel);

        return userRepository.save(user).getEmail();
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
