package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.AnalysisStatus;
import com.example.demo.enums.UserRole;
import com.example.demo.model.Company;
import com.example.demo.model.User;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 05-analise-de-confiabilidade. Cada teste cita o criterio que fecha. */
class AnaliseDeConfiabilidadeSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limpar() {
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - empresa recem-criada tem analise NAO_ENVIADO")
    void c1() throws Exception {
        String email = cadastrarPj();

        assertThat(empresaDe(email).getAnalysisStatus())
            .isEqualTo(AnalysisStatus.NAO_ENVIADO);
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - envio com dossie incompleto e recusado com 400")
    void c2() throws Exception {
        String email = cadastrarPj();

        mockMvc.perform(post("/api/v1/companies/me/analise")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isBadRequest());
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - envio com dossie completo deixa a analise EM_ANALISE")
    void c3() throws Exception {
        String email = cadastrarPjComDossie();

        mockMvc.perform(post("/api/v1/companies/me/analise")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());

        assertThat(empresaDe(email).getAnalysisStatus())
            .isEqualTo(AnalysisStatus.EM_ANALISE);
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - envio sem autenticacao responde 403")
    void c4() throws Exception {
        mockMvc.perform(post("/api/v1/companies/me/analise"))
            .andExpect(status().isForbidden());
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - GET /companies/me devolve o estado da analise")
    void c5() throws Exception {
        String email = cadastrarPj();

        mockMvc.perform(get("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.analysisStatus").value("NAO_ENVIADO"));
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - fila do backoffice com papel comum responde 403")
    void c6() throws Exception {
        String email = cadastrarPj();

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isForbidden());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - a fila lista empresa EM_ANALISE")
    void c7() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].analysisStatus").value("EM_ANALISE"));
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - a fila nao lista empresa NAO_ENVIADO")
    void c8() throws Exception {
        cadastrarPjComDossie();

        mockMvc.perform(get("/api/v1/backoffice/companies")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - aprovacao deixa a analise APROVADA")
    void c9() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        aprovar(empresaDe(email).getId());

        assertThat(empresaDe(email).getAnalysisStatus())
            .isEqualTo(AnalysisStatus.APROVADA);
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - reprovacao deixa a analise REPROVADA")
    void c10() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        reprovar(empresaDe(email).getId(), "CRECI nao confere com o conselho");

        assertThat(empresaDe(email).getAnalysisStatus())
            .isEqualTo(AnalysisStatus.REPROVADA);
    }

    // ---------- C11 ----------

    @Test
    @DisplayName("C11 - reprovacao sem motivo e recusada com 400 nomeando o campo")
    void c11() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        MvcResult result = mockMvc.perform(
                post("/api/v1/backoffice/companies/" + empresaDe(email).getId() + "/reprovar")
                    .header("Authorization", "Bearer " + tokenInterno())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\": \"\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("motivo");
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - o motivo da reprovacao fica gravado")
    void c12() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        reprovar(empresaDe(email).getId(), "CRECI nao confere com o conselho");

        assertThat(empresaDe(email).getRejectionReason())
            .isEqualTo("CRECI nao confere com o conselho");
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - a aprovacao grava quem aprovou e quando")
    void c13() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        String interno = contaInterna();

        mockMvc.perform(post("/api/v1/backoffice/companies/" + empresaDe(email).getId() + "/aprovar")
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(status().isOk());

        Company empresa = empresaDe(email);

        assertThat(empresa.getReviewedBy()).isEqualTo(interno);
        assertThat(empresa.getReviewedAt()).isNotNull();
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - aprovar com papel comum responde 403")
    void c14() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);

        mockMvc.perform(post("/api/v1/backoffice/companies/" + empresaDe(email).getId() + "/aprovar")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isForbidden());
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - empresa aprovada nao pode ser reenviada para analise")
    void c15() throws Exception {
        String email = cadastrarPjComDossie();
        enviarParaAnalise(email);
        aprovar(empresaDe(email).getId());

        mockMvc.perform(post("/api/v1/companies/me/analise")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isBadRequest());
    }

    // ---------- apoio ----------

    private void enviarParaAnalise(String email) throws Exception {
        mockMvc.perform(post("/api/v1/companies/me/analise")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk());
    }

    private void aprovar(Long companyId) throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/companies/" + companyId + "/aprovar")
                .header("Authorization", "Bearer " + tokenInterno()))
            .andExpect(status().isOk());
    }

    private void reprovar(Long companyId, String motivo) throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/companies/" + companyId + "/reprovar")
                .header("Authorization", "Bearer " + tokenInterno())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"%s\"}".formatted(motivo)))
            .andExpect(status().isOk());
    }

    private String cadastrarPj() throws Exception {
        String email = emailUnico();

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
                      "accountType": "PJ",
                      "cnpj": "%s"
                    }
                    """.formatted(email, telefoneUnico(), cnpjUnico())))
            .andExpect(status().isCreated());

        return email;
    }

    private String cadastrarPjComDossie() throws Exception {
        String email = cadastrarPj();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "razaoSocial": "Imobiliaria Horizonte LTDA",
                      "nomeFantasia": "Horizonte Imoveis",
                      "creciNumero": "%s",
                      "creciUf": "PE",
                      "responsavelNome": "Ana Ribeiro",
                      "responsavelCpf": "12345678901",
                      "telefoneComercial": "8133334444",
                      "emailComercial": "contato@horizonte.dev",
                      "cep": "50000-000",
                      "logradouro": "Rua do Sol",
                      "numero": "200",
                      "complemento": "Sala 3",
                      "cidade": "Recife",
                      "uf": "PE"
                    }
                    """.formatted(creciUnico())))
            .andExpect(status().isOk());

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

    private Company empresaDe(String email) {
        return companyRepository.findByUserEmail(email).orElseThrow();
    }

    private String emailUnico() {
        return "a" + SEQUENCIA.getAndIncrement() + "@t.dev";
    }

    private String telefoneUnico() {
        return String.format("11%09d", SEQUENCIA.getAndIncrement());
    }

    private String cnpjUnico() {
        return String.format("%014d", SEQUENCIA.getAndIncrement());
    }

    private String creciUnico() {
        return "J" + SEQUENCIA.getAndIncrement();
    }
}
