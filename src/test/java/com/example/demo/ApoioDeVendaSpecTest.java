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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.SupportStatus;
import com.example.demo.enums.UserRole;
import com.example.demo.model.SupportRequest;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.SupportRequestRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cobre a spec 09-apoio-de-venda. Cada teste cita o criterio que fecha. */
class ApoioDeVendaSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private SupportRequestRepository supportRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        supportRepository.deleteAll();
        announcementRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - dono do anuncio pede apoio e o pedido e criado")
    void c1() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        assertThat(supportRepository.findAll()).hasSize(1);
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - quem nao e dono recebe 403 ao pedir apoio")
    void c2() throws Exception {
        String dono = cadastrarPf();
        Long anuncio = criarAnuncio(dono);

        String estranho = cadastrarPf();

        pedirApoio(anuncio, estranho).andExpect(status().isForbidden());
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - pedido sem autenticacao responde 403")
    void c3() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/apoio"))
            .andExpect(status().isForbidden());
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - pedido nasce com status ABERTO")
    void c4() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        assertThat(supportRepository.findAll().get(0).getStatus())
            .isEqualTo(SupportStatus.ABERTO);
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - o pedido grava o anuncio e o solicitante")
    void c5() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        SupportRequest pedido = supportRepository.findAll().get(0);
        Long solicitante = userRepository.findByEmail(email).orElseThrow().getId();

        // Id sai do proxy lazy sem abrir sessao; getEmail() exigiria inicializar.
        assertThat(pedido.getAnnouncement().getId()).isEqualTo(anuncio);
        assertThat(pedido.getUser().getId()).isEqualTo(solicitante);
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - segundo pedido com um ja aberto responde 409")
    void c6() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());
        pedirApoio(anuncio, email).andExpect(status().isConflict());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - pedido para anuncio inexistente responde 404")
    void c7() throws Exception {
        String email = cadastrarPf();

        pedirApoio(999999L, email).andExpect(status().isNotFound());
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - o dono consulta o pedido vigente do anuncio")
    void c8() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/announcements/" + anuncio + "/apoio")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - consulta sem pedido aberto responde 404")
    void c9() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        mockMvc.perform(get("/api/v1/announcements/" + anuncio + "/apoio")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isNotFound());
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - fila de apoio com papel comum responde 403")
    void c10() throws Exception {
        String email = cadastrarPf();

        mockMvc.perform(get("/api/v1/backoffice/support")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isForbidden());
    }

    // ---------- C11 e C15 ----------

    @Test
    @DisplayName("C11 - a fila lista pedido ABERTO com anuncio e contato")
    void c11() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/backoffice/support")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].announcementId").value(anuncio))
            .andExpect(jsonPath("$.content[0].solicitanteEmail").value(email));
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - a fila nao lista pedido CONCLUIDO")
    void c12() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());
        concluir(supportRepository.findAll().get(0).getId());

        mockMvc.perform(get("/api/v1/backoffice/support")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - concluir deixa o status CONCLUIDO")
    void c13() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        Long pedido = supportRepository.findAll().get(0).getId();
        concluir(pedido);

        assertThat(supportRepository.findById(pedido).orElseThrow().getStatus())
            .isEqualTo(SupportStatus.CONCLUIDO);
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - concluir grava quem atendeu e quando")
    void c14() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());

        String interno = contaInterna();
        Long pedido = supportRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/v1/backoffice/support/" + pedido + "/concluir")
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(status().isOk());

        SupportRequest atualizado = supportRepository.findById(pedido).orElseThrow();

        assertThat(atualizado.getHandledBy()).isEqualTo(interno);
        assertThat(atualizado.getHandledAt()).isNotNull();
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - com o anterior concluido, o dono abre outro pedido")
    void c15() throws Exception {
        String email = cadastrarPf();
        Long anuncio = criarAnuncio(email);

        pedirApoio(anuncio, email).andExpect(status().isOk());
        concluir(supportRepository.findAll().get(0).getId());

        pedirApoio(anuncio, email).andExpect(status().isOk());
    }

    // ---------- apoio ----------

    private org.springframework.test.web.servlet.ResultActions pedirApoio(Long anuncio, String email)
        throws Exception {

        return mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/apoio")
            .header("Authorization", "Bearer " + tokenDe(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mensagem\": \"Preciso vender rapido\"}"));
    }

    private void concluir(Long pedido) throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/support/" + pedido + "/concluir")
                .header("Authorization", "Bearer " + tokenInterno()))
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

    private String cadastrarPf() throws Exception {
        String email = emailUnico();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Dona do imovel",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "PF"
                    }
                    """.formatted(email, telefoneUnico())))
            .andExpect(status().isCreated());

        return email;
    }

    private String contaInterna() {
        User interna = new User();

        interna.setFullname("Equipe interna");
        interna.setEmail(emailUnico());
        interna.setPhone(telefoneUnico());
        interna.setPassword(passwordEncoder.encode("Senha123"));
        interna.setAccountType(AccountType.INTERNO);
        interna.setAccountStatus(AccountStatus.ATIVO);
        interna.setRole(UserRole.INTERNO);

        return userRepository.save(interna).getEmail();
    }

    private String tokenInterno() {
        return tokenDe(contaInterna());
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }

    private String emailUnico() {
        return "s" + SEQUENCIA.getAndIncrement() + "@t.dev";
    }

    private String telefoneUnico() {
        return String.format("11%09d", SEQUENCIA.getAndIncrement());
    }
}
