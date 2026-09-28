package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.config.AccountBootstrap;
import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 01-contas-tipadas. Cada teste cita o criterio que fecha. */
class ContasTipadasSpecTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountBootstrap bootstrap;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        // deleteAll carrega as entidades, entao o cascade para companies se aplica.
        repository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - cadastro sem tipo de conta e recusado com 400 nomeando o campo")
    void c1() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(null, null)))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("accountType");
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - tipo de conta fora do conjunto aceito e recusado com 400")
    void c2() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoComTipoCru("SINDICATO")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C2 - INTERNO nao e aceito pelo cadastro publico")
    void c2Interno() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("INTERNO", null)))
            .andExpect(status().isBadRequest());
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - cadastro PF persiste conta com tipo PF")
    void c3() throws Exception {
        String email = cadastrar("PF", null);

        assertThat(buscar(email).getAccountType()).isEqualTo(AccountType.PF);
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - cadastro PJ persiste conta com tipo PJ")
    void c4() throws Exception {
        String email = cadastrar("PJ", cnpjUnico());

        assertThat(buscar(email).getAccountType()).isEqualTo(AccountType.PJ);
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - conta PF nasce com status ATIVO")
    void c5() throws Exception {
        String email = cadastrar("PF", null);

        assertThat(buscar(email).getAccountStatus()).isEqualTo(AccountStatus.ATIVO);
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - conta PJ nasce com status ATIVO")
    void c6() throws Exception {
        String email = cadastrar("PJ", cnpjUnico());

        assertThat(buscar(email).getAccountStatus()).isEqualTo(AccountStatus.ATIVO);
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - cadastro PJ sem CNPJ e recusado com 400")
    void c7() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("PJ", null)))
            .andExpect(status().isBadRequest());
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - CNPJ ja existente e recusado com 409")
    void c8() throws Exception {
        String cnpj = cnpjUnico();

        cadastrar("PJ", cnpj);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("PJ", cnpj)))
            .andExpect(status().isConflict());
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - apos a carga, conta preexistente tem status ATIVO")
    void c9() {
        User legada = new User();

        legada.setFullname("Conta anterior a spec");
        legada.setEmail("legada@attus.dev");
        legada.setPhone("11900000000");
        legada.setPassword(passwordEncoder.encode("Senha123"));

        repository.save(legada);

        bootstrap.backfillExistingAccounts();

        User recarregada = buscar("legada@attus.dev");

        assertThat(recarregada.getAccountStatus()).isEqualTo(AccountStatus.ATIVO);
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - o JWT carrega o tipo de conta")
    void c10() throws Exception {
        String email = cadastrar("PF", null);

        String token = jwtService.generateToken(buscar(email));

        assertThat(claim(token, "accountType")).isEqualTo("PF");
    }

    // ---------- C11 ----------

    @Test
    @DisplayName("C11 - o JWT carrega o papel")
    void c11() throws Exception {
        String email = cadastrar("PF", null);

        String token = jwtService.generateToken(buscar(email));

        assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.COMUM.name());
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - backoffice responde 403 para papel diferente de INTERNO")
    void c12() throws Exception {
        String email = cadastrar("PF", null);

        String token = jwtService.generateToken(buscar(email));

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - backoffice nao responde 403 para papel INTERNO")
    void c13() throws Exception {
        String token = jwtService.generateToken(contaInterna());

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - cadastro publico nunca produz papel INTERNO")
    void c14() throws Exception {
        String email = emailUnico();

        String corpo = """
            {
              "fullname": "Tentativa de escalada",
              "email": "%s",
              "phone": "%s",
              "password": "Senha123",
              "confirmPassword": "Senha123",
              "photo": "",
              "accountType": "PF",
              "role": "INTERNO",
              "accountStatus": "ATIVO"
            }
            """.formatted(email, telefoneUnico());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
            .andExpect(status().isCreated());

        assertThat(buscar(email).getRole()).isEqualTo(UserRole.COMUM);
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - listagem publica de anuncios segue respondendo 200")
    void c15() throws Exception {
        mockMvc.perform(get("/api/v1/announcements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    // ---------- apoio ----------

    private String cadastrar(String tipo, String cnpj) throws Exception {
        String email = emailUnico();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoCompleto(email, telefoneUnico(), tipo, cnpj)))
            .andExpect(status().isCreated());

        return email;
    }

    private User contaInterna() {
        User interna = new User();

        interna.setFullname("Equipe interna");
        interna.setEmail(emailUnico());
        interna.setPhone(telefoneUnico());
        interna.setPassword(passwordEncoder.encode("Senha123"));
        interna.setAccountType(AccountType.INTERNO);
        interna.setAccountStatus(AccountStatus.ATIVO);
        interna.setRole(UserRole.INTERNO);

        return repository.save(interna);
    }

    private User buscar(String email) {
        return repository.findByEmail(email).orElseThrow();
    }

    private String claim(String token, String nome) throws Exception {
        String payload = token.split("\\.")[1];

        JsonNode json = mapper.readTree(java.util.Base64.getUrlDecoder().decode(payload));

        return json.path(nome).asText();
    }

    private String corpo(String tipo, String cnpj) {
        return corpoCompleto(emailUnico(), telefoneUnico(), tipo, cnpj);
    }

    private String corpoCompleto(String email, String phone, String tipo, String cnpj) {
        String tipoJson = tipo == null ? "null" : "\"" + tipo + "\"";
        String cnpjJson = cnpj == null ? "null" : "\"" + cnpj + "\"";

        return """
            {
              "fullname": "Conta de teste",
              "email": "%s",
              "phone": "%s",
              "password": "Senha123",
              "confirmPassword": "Senha123",
              "photo": "",
              "accountType": %s,
              "cnpj": %s
            }
            """.formatted(email, phone, tipoJson, cnpjJson);
    }

    private String corpoComTipoCru(String tipo) {
        return corpoCompleto(emailUnico(), telefoneUnico(), tipo, null);
    }

    /** Contador, nao relogio: telefone cabe em 15 caracteres e precisa ser unico. */
    private static final java.util.concurrent.atomic.AtomicLong SEQUENCIA =
        new java.util.concurrent.atomic.AtomicLong(1);

    /** Coluna de e-mail tem 40 caracteres: UUID nao cabe. */
    private String emailUnico() {
        return "c" + SEQUENCIA.getAndIncrement() + "@t.dev";
    }

    private String cnpjUnico() {
        return String.format("%014d", SEQUENCIA.getAndIncrement());
    }

    private String telefoneUnico() {
        return String.format("11%09d", SEQUENCIA.getAndIncrement());
    }
}
