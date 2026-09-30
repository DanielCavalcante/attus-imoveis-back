package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.AccountBootstrap;
import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.SupportRequestRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.services.SupportRequestService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManagerFactory;

/** Cobre a spec 14-paginacao-e-consultas-das-filas. Cada teste cita o criterio que fecha. */
class PaginacaoDasFilasSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SupportRequestRepository supportRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private SupportRequestService supportRequestService;

    @Autowired
    private AccountBootstrap bootstrap;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        supportRepository.deleteAll();
        announcementRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1, C2, C3, C4, C5 ----------

    @Test
    @DisplayName("C1 - a fila de empresas aceita page e size")
    void c1() throws Exception {
        mockMvc.perform(get("/api/v1/backoffice/companies")
                .param("page", "0").param("size", "2")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("C2 - a resposta traz o total de elementos")
    void c2() throws Exception {
        enfileirarEmpresas(3);

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .param("page", "0").param("size", "2")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("C3 - com tres na fila e size=2, a primeira pagina traz duas")
    void c3() throws Exception {
        enfileirarEmpresas(3);

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .param("page", "0").param("size", "2")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("C4 - a segunda pagina traz a restante")
    void c4() throws Exception {
        enfileirarEmpresas(3);

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .param("page", "1").param("size", "2")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("C5 - a fila de empresas vem da mais antiga para a mais nova")
    void c5() throws Exception {
        List<Long> ordemDeChegada = enfileirarEmpresas(3);

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(jsonPath("$.content[0].id").value(ordemDeChegada.get(0))
            ).andExpect(jsonPath("$.content[2].id").value(ordemDeChegada.get(2)));
    }

    // ---------- C6 e C7 ----------

    @Test
    @DisplayName("C6 - a fila de apoio aceita page e size")
    void c6() throws Exception {
        enfileirarApoios(3);

        mockMvc.perform(get("/api/v1/backoffice/support")
                .param("page", "0").param("size", "2")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("C7 - a fila de apoio vem da mais antiga para a mais nova")
    void c7() throws Exception {
        List<Long> ordemDeChegada = enfileirarApoios(3);

        mockMvc.perform(get("/api/v1/backoffice/support")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(jsonPath("$.content[0].announcementId").value(ordemDeChegada.get(0)))
            .andExpect(jsonPath("$.content[2].announcementId").value(ordemDeChegada.get(2)));
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - a fila de apoio com cinco pedidos usa no maximo tres consultas")
    void c8() throws Exception {
        enfileirarApoios(5);

        Statistics stats = estatisticas();
        stats.clear();

        // Monta a linha inteira da fila, que desreferencia anuncio e solicitante.
        supportRequestService.findOpenRequests(PageRequest.of(0, 20))
            .forEach(linha -> {
                assertThat(linha.announcementTitle()).isNotBlank();
                assertThat(linha.solicitanteEmail()).isNotBlank();
            });

        // Sem EntityGraph seriam 1 + 5*2 = 11. O teto de 3 cobre a consulta da
        // pagina, a de contagem e uma folga, sem dar espaco para N+1.
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    // ---------- C9 e C10 ----------

    @Test
    @DisplayName("C9 - a carga seleciona apenas contas sem tipo")
    void c9() {
        contaTipada();
        Long semTipo = contaSemTipo();

        bootstrap.backfillExistingAccounts();

        assertThat(userRepository.findById(semTipo).orElseThrow().getAccountType())
            .isEqualTo(AccountType.PF);
    }

    @Test
    @DisplayName("C10 - a carga com cinco contas tipadas e uma sem tipo carrega uma linha")
    void c10() {
        for (int i = 0; i < 5; i++) {
            contaTipada();
        }

        contaSemTipo();

        assertThat(userRepository.findAllByAccountTypeIsNull()).hasSize(1);
    }

    // ---------- C11 e C12 ----------

    @Test
    @DisplayName("C11 - carregar uma conta pelo e-mail dispara no maximo uma consulta")
    void c11() {
        String email = cadastrarDireto();

        Statistics stats = estatisticas();
        stats.clear();

        userRepository.findByEmail(email).orElseThrow();

        // Com o @OneToOne inverso em User eram duas: a conta e a empresa dela.
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("C12 - remover a conta remove a empresa dela")
    void c12() throws Exception {
        String email = cadastrarPj();
        Long empresa = companyRepository.findByUserEmail(email).orElseThrow().getId();

        userRepository.delete(userRepository.findByEmail(email).orElseThrow());

        assertThat(companyRepository.findById(empresa)).isEmpty();
    }

    // ---------- apoio ----------

    private String cadastrarDireto() {
        User user = new User();

        user.setFullname("Conta de teste");
        user.setEmail("c" + SEQUENCIA.getAndIncrement() + "@t.dev");
        user.setPhone(String.format("11%09d", SEQUENCIA.getAndIncrement()));
        user.setPassword(passwordEncoder.encode("Senha123"));
        user.setAccountType(AccountType.PF);
        user.setAccountStatus(AccountStatus.ATIVO);
        user.setRole(UserRole.COMUM);

        return userRepository.save(user).getEmail();
    }

    private Statistics estatisticas() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private List<Long> enfileirarEmpresas(int quantas) throws Exception {
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < quantas; i++) {
            String email = cadastrarPj();
            enviarParaAnalise(email);
            ids.add(companyRepository.findByUserEmail(email).orElseThrow().getId());
        }

        return ids;
    }

    private List<Long> enfileirarApoios(int quantos) throws Exception {
        List<Long> anuncios = new ArrayList<>();

        for (int i = 0; i < quantos; i++) {
            String email = cadastrarPf();
            Long anuncio = criarAnuncio(email);

            mockMvc.perform(post("/api/v1/announcements/" + anuncio + "/apoio")
                    .header("Authorization", "Bearer " + tokenDe(email))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mensagem\": \"Preciso vender rapido\"}"))
                .andExpect(status().isOk());

            anuncios.add(anuncio);

            // Garante ordem estavel por createdAt entre pedidos consecutivos.
            Thread.sleep(15);
        }

        return anuncios;
    }

    private void enviarParaAnalise(String email) throws Exception {
        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "razaoSocial": "Imobiliaria %d LTDA",
                      "nomeFantasia": "Fantasia %d",
                      "creciNumero": "J%d",
                      "creciUf": "PE",
                      "responsavelNome": "Ana Ribeiro",
                      "responsavelCpf": "12345678901",
                      "telefoneComercial": "8133334444",
                      "emailComercial": "contato@horizonte.dev",
                      "cep": "50000-000",
                      "logradouro": "Rua do Sol",
                      "numero": "200",
                      "complemento": "",
                      "cidade": "Recife",
                      "uf": "PE"
                    }
                    """.formatted(SEQUENCIA.get(), SEQUENCIA.get(), SEQUENCIA.getAndIncrement())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/companies/me/analise")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());
    }

    private Long criarAnuncio(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Apartamento %d",
                      "description": "Anuncio da suite",
                      "image": "/api/v1/imagens/foto-de-teste.png",
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

    private String cadastrarPj() throws Exception {
        return cadastrar("PJ", "\"%014d\"".formatted(SEQUENCIA.getAndIncrement()));
    }

    private String cadastrarPf() throws Exception {
        return cadastrar("PF", "null");
    }

    private String cadastrar(String tipo, String cnpj) throws Exception {
        String email = "n" + SEQUENCIA.getAndIncrement() + "@t.dev";

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

    private void contaTipada() {
        salvar(AccountType.PF, AccountStatus.ATIVO, UserRole.COMUM);
    }

    private Long contaSemTipo() {
        return salvar(null, null, null);
    }

    private Long salvar(AccountType tipo, AccountStatus status, UserRole papel) {
        User user = new User();

        user.setFullname("Conta de teste");
        user.setEmail("b" + SEQUENCIA.getAndIncrement() + "@t.dev");
        user.setPhone(String.format("11%09d", SEQUENCIA.getAndIncrement()));
        user.setPassword(passwordEncoder.encode("Senha123"));
        user.setAccountType(tipo);
        user.setAccountStatus(status);
        user.setRole(papel);

        return userRepository.save(user).getId();
    }

    private String tokenInterno() {
        User interna = new User();

        interna.setFullname("Equipe interna");
        interna.setEmail("i" + SEQUENCIA.getAndIncrement() + "@t.dev");
        interna.setPhone(String.format("11%09d", SEQUENCIA.getAndIncrement()));
        interna.setPassword(passwordEncoder.encode("Senha123"));
        interna.setAccountType(AccountType.INTERNO);
        interna.setAccountStatus(AccountStatus.ATIVO);
        interna.setRole(UserRole.INTERNO);

        return jwtService.generateToken(userRepository.save(interna));
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
