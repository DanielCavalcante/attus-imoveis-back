package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.ResultActions;

import com.example.demo.enums.UserRole;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.SubscriptionRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 45-a-conta-so-mexe-em-si-mesma. Cada teste cita o criterio. */
class ContaSoMexeEmSiSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(110000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtService jwtService;

    /** Quem age. */
    private String eu;
    private Long meuId;

    /** Quem nao tem nada a ver com isso. */
    private String outra;
    private Long idDaOutra;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        eu = cadastrar();
        meuId = idDe(eu);

        outra = cadastrar();
        idDaOutra = idDe(outra);
    }

    // ---------- C1 a C3: o que nao se alcanca ----------

    @Test
    @DisplayName("C1 - editar outra conta e recusado, e o dado dela nao muda")
    void c1() throws Exception {
        editar(idDaOutra, "Invadida", eu).andExpect(status().isNotFound());

        assertThat(userRepository.findById(idDaOutra))
            .get()
            .satisfies(u -> assertThat(u.getFullname()).isNotEqualTo("Invadida"));
    }

    @Test
    @DisplayName("C2 - excluir outra conta e recusado, e ela continua existindo")
    void c2() throws Exception {
        mockMvc.perform(delete("/api/v1/users/" + idDaOutra)
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andExpect(status().isNotFound());

        assertThat(userRepository.findById(idDaOutra)).isPresent();
    }

    @Test
    @DisplayName("C3 - ler o detalhe de outra conta e recusado")
    void c3() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + idDaOutra)
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andExpect(status().isNotFound());
    }

    // ---------- C4: a listagem ----------

    @Test
    @DisplayName("C4 - conta comum nao alcanca a lista de contas")
    void c4() throws Exception {
        // Ela devolvia o e-mail de todo mundo para qualquer sessao.
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andExpect(status().isForbidden());
    }

    // ---------- C5 a C7: a propria conta ----------

    @Test
    @DisplayName("C5 - editar a propria conta continua funcionando")
    void c5() throws Exception {
        editar(meuId, "Nome Corrigido", eu)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullname").value("Nome Corrigido"));
    }

    @Test
    @DisplayName("C6 - excluir a propria conta continua funcionando")
    void c6() throws Exception {
        mockMvc.perform(delete("/api/v1/users/" + meuId)
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andExpect(status().isNoContent());

        assertThat(userRepository.findById(meuId)).isEmpty();
    }

    @Test
    @DisplayName("C7 - ler o proprio detalhe continua funcionando")
    void c7() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + meuId)
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(eu));
    }

    // ---------- C8 e C9: o que a recusa conta, e quem decide ----------

    @Test
    @DisplayName("C8 - a recusa nao distingue conta alheia de conta inexistente")
    void c8() throws Exception {
        int alheia = mockMvc.perform(get("/api/v1/users/" + idDaOutra)
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andReturn().getResponse().getStatus();

        int inexistente = mockMvc.perform(get("/api/v1/users/99999999")
                .header("Authorization", "Bearer " + tokenDe(eu)))
            .andReturn().getResponse().getStatus();

        // Responder "existe, mas nao e sua" enumera a base inteira com id
        // sequencial.
        assertThat(alheia).isEqualTo(inexistente).isEqualTo(404);
    }

    @Test
    @DisplayName("C9 - id no corpo nao muda quem e alterado")
    void c9() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + meuId)
                .header("Authorization", "Bearer " + tokenDe(eu))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "id": %d,
                      "fullname": "Tentando pelo corpo",
                      "email": "%s",
                      "phone": "%s",
                      "photo": ""
                    }
                    """.formatted(idDaOutra, eu, telefoneDe(eu))))
            .andExpect(status().isOk());

        // Quem manda e a sessao: a outra conta nao foi tocada.
        assertThat(userRepository.findById(idDaOutra))
            .get()
            .satisfies(u -> assertThat(u.getFullname()).isNotEqualTo("Tentando pelo corpo"));
    }

    // ---------- C10: a conta interna ----------

    @Test
    @DisplayName("C10 - a conta interna continua alcancando o backoffice")
    void c10() throws Exception {
        String interno = cadastrar();
        User user = userRepository.findByEmail(interno).orElseThrow();

        user.setRole(UserRole.INTERNO);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/v1/backoffice/ping")
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(status().isOk());
    }

    // ---------- C11: a mensagem certa ----------

    @Test
    @DisplayName("C11 - corpo sem e-mail responde 400, e nao 403")
    void c11() throws Exception {
        // Antes disto o corpo incompleto chegava a `email.equals(...)` e
        // estourava dentro da validacao: quem integrava lia a falha como
        // problema de permissao, e nao como campo faltando.
        mockMvc.perform(put("/api/v1/users/" + meuId)
                .header("Authorization", "Bearer " + tokenDe(eu))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullname": "Sem e-mail", "phone": "%s", "photo": ""}
                    """.formatted(telefoneDe(eu))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("E-mail")));
    }

    // ---------- apoio ----------

    private ResultActions editar(Long id, String nome, String conta) throws Exception {
        String email = userRepository.findById(id)
            .map(User::getEmail)
            .orElse("ninguem@t.dev");

        String telefone = userRepository.findById(id)
            .map(User::getPhone)
            .orElse("11900000000");

        return mockMvc.perform(put("/api/v1/users/" + id)
            .header("Authorization", "Bearer " + tokenDe(conta))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullname": "%s",
                  "email": "%s",
                  "phone": "%s",
                  "photo": ""
                }
                """.formatted(nome, email, telefone)));
    }

    private String cadastrar() throws Exception {
        String email = "conta" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta da spec 45",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "PF",
                      "cpf": "%011d"
                    }
                    """.formatted(
                        email,
                        String.format("11%09d", SEQUENCIA.getAndIncrement()),
                        SEQUENCIA.getAndIncrement())))
            .andExpect(status().isCreated());

        return email;
    }

    private Long idDe(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private String telefoneDe(String email) {
        return userRepository.findByEmail(email).orElseThrow().getPhone();
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
