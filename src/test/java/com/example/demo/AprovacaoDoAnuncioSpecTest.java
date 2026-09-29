package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.example.demo.enums.AnalysisStatus;
import com.example.demo.model.Company;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 28-aprovacao-manda-no-anuncio. Cada teste cita o criterio. */
class AprovacaoDoAnuncioSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(60000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JwtService jwtService;

    private String empresa;
    private String pessoaFisica;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        empresa = cadastrar("PJ");
        pessoaFisica = cadastrar("PF");
    }

    // ---------- C1 a C3: os tres estados que nao publicam ----------

    @Test
    @DisplayName("C1 - empresa em analise nao publica")
    void c1() throws Exception {
        estado(empresa, AnalysisStatus.EM_ANALISE);

        publicar(empresa).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C2 - empresa que nem enviou o dossie nao publica")
    void c2() throws Exception {
        estado(empresa, AnalysisStatus.NAO_ENVIADO);

        publicar(empresa).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C3 - empresa reprovada nao publica")
    void c3() throws Exception {
        estado(empresa, AnalysisStatus.REPROVADA);

        publicar(empresa).andExpect(status().isForbidden());
    }

    // ---------- C4 a C6: quem publica, e o que a recusa nao deixa ----------

    @Test
    @DisplayName("C4 - empresa aprovada publica")
    void c4() throws Exception {
        estado(empresa, AnalysisStatus.APROVADA);

        publicar(empresa).andExpect(status().isOk());
    }

    @Test
    @DisplayName("C5 - pessoa fisica publica sem que a analise seja consultada")
    void c5() throws Exception {
        // Nenhuma empresa existe para esta conta: se o portao a consultasse,
        // a ausencia de dossie a trancaria.
        assertThat(companyRepository.findByUserEmail(pessoaFisica)).isEmpty();

        publicar(pessoaFisica).andExpect(status().isOk());
    }

    @Test
    @DisplayName("C6 - a recusa nao grava nada")
    void c6() throws Exception {
        estado(empresa, AnalysisStatus.EM_ANALISE);

        long antes = announcementRepository.count();

        publicar(empresa).andExpect(status().isForbidden());

        assertThat(announcementRepository.count()).isEqualTo(antes);
    }

    // ---------- C7: a recusa explica ----------

    @Test
    @DisplayName("C7 - o corpo do 403 nomeia o estado da analise")
    void c7() throws Exception {
        estado(empresa, AnalysisStatus.REPROVADA);

        publicar(empresa)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString(
                    AnalysisStatus.REPROVADA.getDescription())));
    }

    // ---------- C8 a C10: o que a regra alcanca, e o que ela nao alcanca ----------

    @Test
    @DisplayName("C8 - reativar anuncio de empresa nao aprovada e recusado")
    void c8() throws Exception {
        estado(empresa, AnalysisStatus.APROVADA);
        long id = idDoPublicado(empresa);

        mockMvc.perform(patch("/api/v1/announcements/" + id + "/inativar")
                .header("Authorization", "Bearer " + tokenDe(empresa)))
            .andExpect(status().isNoContent());

        // A aprovacao cai depois de o anuncio ja ter saido do ar.
        estado(empresa, AnalysisStatus.REPROVADA);

        mockMvc.perform(patch("/api/v1/announcements/" + id + "/reativar")
                .header("Authorization", "Bearer " + tokenDe(empresa)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C9 - editar anuncio existente continua permitido")
    void c9() throws Exception {
        estado(empresa, AnalysisStatus.APROVADA);
        long id = idDoPublicado(empresa);

        estado(empresa, AnalysisStatus.REPROVADA);

        mockMvc.perform(put("/api/v1/announcements/" + id)
                .header("Authorization", "Bearer " + tokenDe(empresa))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDoAnuncio("Titulo corrigido")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Titulo corrigido"));
    }

    @Test
    @DisplayName("C10 - anuncio publicado antes da regra continua no ar")
    void c10() throws Exception {
        estado(empresa, AnalysisStatus.APROVADA);
        idDoPublicado(empresa);

        estado(empresa, AnalysisStatus.REPROVADA);

        mockMvc.perform(get("/api/v1/announcements").param("q", "Casa da spec 28"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- C11 e C12: o aviso, e de quem e a palavra final ----------

    @Test
    @DisplayName("C11 - a quota diz que nao pode publicar, e por que")
    void c11() throws Exception {
        estado(empresa, AnalysisStatus.EM_ANALISE);

        mockMvc.perform(get("/api/v1/announcements/me/quota")
                .header("Authorization", "Bearer " + tokenDe(empresa)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.podePublicar").value(false))
            .andExpect(jsonPath("$.motivo").value("EMPRESA_EM_ANALISE"));
    }

    @Test
    @DisplayName("C11 - aprovada, a quota volta a dizer que pode")
    void c11Aprovada() throws Exception {
        estado(empresa, AnalysisStatus.APROVADA);

        mockMvc.perform(get("/api/v1/announcements/me/quota")
                .header("Authorization", "Bearer " + tokenDe(empresa)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.podePublicar").value(true))
            .andExpect(jsonPath("$.motivo").doesNotExist());
    }

    @Test
    @DisplayName("C12 - a recusa vem do servidor, sem passar por tela nenhuma")
    void c12() throws Exception {
        estado(empresa, AnalysisStatus.EM_ANALISE);

        // Requisicao crua, com token valido e sem nada do front: e o servidor
        // que tranca, entao a tela nao e o portao.
        publicar(empresa).andExpect(status().isForbidden());
    }

    // ---------- apoio ----------

    private void estado(String email, AnalysisStatus status) {
        Company company = companyRepository.findByUserEmail(email).orElseThrow();

        company.setAnalysisStatus(status);
        companyRepository.saveAndFlush(company);
    }

    private org.springframework.test.web.servlet.ResultActions publicar(String email)
        throws Exception {

        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoDoAnuncio("Casa da spec 28")));
    }

    /** O id sai da propria resposta: ler do repositorio esbarra no lazy do dono. */
    private long idDoPublicado(String email) throws Exception {
        String corpo = publicar(email)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(corpo)
            .get("id")
            .asLong();
    }

    private String corpoDoAnuncio(String titulo) {
        return """
            {
              "title": "%s",
              "description": "Anuncio da suite",
              "image": "https://images.unsplash.com/foto.jpg",
              "price": 300000,
              "city": "João Pessoa",
              "state": "PB",
              "neighborhood": "Manaíra",
              "street": "Rua do Sol",
              "streetNumber": 100,
              "cep": "58038-000",
              "propertyType": "APARTMENT",
              "reason": "SALE",
              "rooms": 2,
              "bathRooms": 1,
              "area": 70
            }
            """.formatted(titulo);
    }

    private String cadastrar(String tipo) throws Exception {
        String email = "apr" + SEQUENCIA.getAndIncrement() + "@t.dev";

        String documento = "PJ".equals(tipo)
            ? "\"cnpj\": \"%014d\"".formatted(SEQUENCIA.getAndIncrement())
            : "\"cpf\": \"%011d\"".formatted(SEQUENCIA.getAndIncrement());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta da spec 28",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "%s",
                      %s
                    }
                    """.formatted(
                        email,
                        String.format("11%09d", SEQUENCIA.getAndIncrement()),
                        tipo,
                        documento)))
            .andExpect(status().isCreated());

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(
            userRepository.findByEmail(email).orElseThrow()
        );
    }
}
