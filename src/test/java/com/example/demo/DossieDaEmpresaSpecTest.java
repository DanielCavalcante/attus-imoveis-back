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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.demo.model.Company;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 02-dossie-da-empresa. Cada teste cita o criterio que fecha. */
class DossieDaEmpresaSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void limpar() {
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - cadastro PJ so com CNPJ e aceito com 201")
    void c1() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cadastro(emailUnico(), "PJ", cnpjUnico())))
            .andExpect(status().isCreated());
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - conta PJ passa a ter registro de empresa associado")
    void c2() throws Exception {
        String email = cadastrarPj();

        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        assertThat(companyRepository.findByUserId(userId)).isPresent();
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - conta PF nao gera registro de empresa")
    void c3() throws Exception {
        String email = emailUnico();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cadastro(email, "PF", null)))
            .andExpect(status().isCreated());

        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        assertThat(companyRepository.findByUserId(userId)).isEmpty();
    }

    // ---------- C4 ----------

    @Test
    @DisplayName("C4 - empresa recem-criada tem dossie incompleto")
    void c4() throws Exception {
        String email = cadastrarPj();

        assertThat(empresaDe(email).isDossieCompleto()).isFalse();
    }

    // ---------- C5 ----------

    @Test
    @DisplayName("C5 - GET /companies/me com conta PJ devolve 200 com o CNPJ")
    void c5() throws Exception {
        String cnpj = cnpjUnico();
        String email = cadastrarPj(cnpj);

        mockMvc.perform(get("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cnpj").value(cnpj));
    }

    // ---------- C6 ----------

    @Test
    @DisplayName("C6 - GET /companies/me sem autorizacao responde 403")
    void c6() throws Exception {
        mockMvc.perform(get("/api/v1/companies/me"))
            .andExpect(status().isForbidden());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - GET /companies/me com conta PF responde 404")
    void c7() throws Exception {
        String email = cadastrarPf();

        mockMvc.perform(get("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email)))
            .andExpect(status().isNotFound());
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - PUT /companies/me com dossie completo responde 200")
    void c8() throws Exception {
        String email = cadastrarPj();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieCompleto(creciUnico())))
            .andExpect(status().isOk());
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - apos o PUT, os campos do dossie estao gravados")
    void c9() throws Exception {
        String email = cadastrarPj();
        String creci = creciUnico();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieCompleto(creci)))
            .andExpect(status().isOk());

        Company empresa = empresaDe(email);

        assertThat(empresa.getRazaoSocial()).isEqualTo("Imobiliaria Horizonte LTDA");
        assertThat(empresa.getNomeFantasia()).isEqualTo("Horizonte Imoveis");
        assertThat(empresa.getCreciNumero()).isEqualTo(creci);
        assertThat(empresa.getResponsavelNome()).isEqualTo("Ana Ribeiro");
        assertThat(empresa.getCidade()).isEqualTo("Recife");
    }

    // ---------- C10 ----------

    @Test
    @DisplayName("C10 - apos o PUT completo, a empresa fica marcada como completa")
    void c10() throws Exception {
        String email = cadastrarPj();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieCompleto(creciUnico())))
            .andExpect(status().isOk());

        assertThat(empresaDe(email).isDossieCompleto()).isTrue();
    }

    // ---------- C11 ----------

    @Test
    @DisplayName("C11 - PUT sem razao social e recusado com 400 nomeando o campo")
    void c11() throws Exception {
        String email = cadastrarPj();

        MvcResult result = mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieSem("razaoSocial", creciUnico())))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("razaoSocial");
    }

    // ---------- C12 ----------

    @Test
    @DisplayName("C12 - PUT sem numero de CRECI e recusado com 400 nomeando o campo")
    void c12() throws Exception {
        String email = cadastrarPj();

        MvcResult result = mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieSem("creciNumero", creciUnico())))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("creciNumero");
    }

    // ---------- C13 ----------

    @Test
    @DisplayName("C13 - CRECI ja usado por outra empresa e recusado com 409")
    void c13() throws Exception {
        String creci = creciUnico();

        String primeira = cadastrarPj();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(primeira))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieCompleto(creci)))
            .andExpect(status().isOk());

        String segunda = cadastrarPj();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(segunda))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieCompleto(creci)))
            .andExpect(status().isConflict());
    }

    // ---------- C14 ----------

    @Test
    @DisplayName("C14 - o PUT altera apenas a empresa da conta autenticada")
    void c14() throws Exception {
        String alvo = cadastrarPj();
        String vizinha = cadastrarPj();

        mockMvc.perform(put("/api/v1/companies/me")
                .header("Authorization", "Bearer " + tokenDe(alvo))
                .contentType(MediaType.APPLICATION_JSON)
                .content(dossieCompleto(creciUnico())))
            .andExpect(status().isOk());

        assertThat(empresaDe(vizinha).getRazaoSocial()).isNull();
    }

    // ---------- C15 ----------

    @Test
    @DisplayName("C15 - CNPJ com letras e gravado e relido sem perda")
    void c15() throws Exception {
        String cnpjAlfanumerico = "AB" + SEQUENCIA.getAndIncrement() + "CD0001X";

        String email = cadastrarPj(cnpjAlfanumerico);

        assertThat(empresaDe(email).getCnpj()).isEqualTo(cnpjAlfanumerico);
    }

    // ---------- apoio ----------

    private String cadastrarPj() throws Exception {
        return cadastrarPj(cnpjUnico());
    }

    private String cadastrarPj(String cnpj) throws Exception {
        String email = emailUnico();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cadastro(email, "PJ", cnpj)))
            .andExpect(status().isCreated());

        return email;
    }

    private String cadastrarPf() throws Exception {
        String email = emailUnico();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cadastro(email, "PF", null)))
            .andExpect(status().isCreated());

        return email;
    }

    private Company empresaDe(String email) {
        return companyRepository.findByUserEmail(email).orElseThrow();
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }

    private String cadastro(String email, String tipo, String cnpj) {
        String cnpjJson = cnpj == null ? "null" : "\"" + cnpj + "\"";

        return """
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
            """.formatted(email, telefoneUnico(), tipo, cnpjJson);
    }

    private String dossieCompleto(String creci) {
        return dossie(creci, null);
    }

    private String dossieSem(String campo, String creci) {
        return dossie(creci, campo);
    }

    /** Monta o dossie completo e, quando pedido, esvazia um campo so. */
    private String dossie(String creci, String campoVazio) {
        java.util.Map<String, String> campos = new java.util.LinkedHashMap<>();

        campos.put("razaoSocial", "Imobiliaria Horizonte LTDA");
        campos.put("nomeFantasia", "Horizonte Imoveis");
        campos.put("creciNumero", creci);
        campos.put("creciUf", "PE");
        campos.put("responsavelNome", "Ana Ribeiro");
        campos.put("responsavelCpf", "12345678901");
        campos.put("telefoneComercial", "8133334444");
        campos.put("emailComercial", "contato@horizonte.dev");
        campos.put("cep", "50000-000");
        campos.put("logradouro", "Rua do Sol");
        campos.put("numero", "200");
        campos.put("complemento", "Sala 3");
        campos.put("cidade", "Recife");
        campos.put("uf", "PE");

        if (campoVazio != null) {
            campos.put(campoVazio, "");
        }

        String corpo = campos.entrySet().stream()
            .map(e -> "\"%s\": \"%s\"".formatted(e.getKey(), e.getValue()))
            .reduce((a, b) -> a + ",\n  " + b)
            .orElse("");

        return "{\n  " + corpo + "\n}";
    }

    private String emailUnico() {
        return "e" + SEQUENCIA.getAndIncrement() + "@t.dev";
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
