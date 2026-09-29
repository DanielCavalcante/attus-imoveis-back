package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.demo.enums.AnalysisStatus;
import com.example.demo.model.Company;
import com.example.demo.model.Plan;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.PlanRepository;
import com.example.demo.repositories.SubscriptionRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/** Cobre a spec 30-vinculo-de-plano-da-empresa. Cada teste cita o criterio. */
class VinculoDePlanoSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(70000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtService jwtService;

    /** Empresa aprovada, sem plano nenhum ainda. */
    private String empresa;
    private Long empresaId;

    private String interno;

    private Long planoDeDois;
    private Long planoDeCinco;
    private Long planoSemTeto;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
        planRepository.deleteAll();

        empresa = cadastrar("PJ");
        aprovarSemPlano(empresa);
        empresaId = companyRepository.findByUserEmail(empresa).orElseThrow().getId();

        interno = cadastrarInterno();

        planoDeDois = plano("Dois anuncios", 2, true);
        planoDeCinco = plano("Cinco anuncios", 5, true);
        planoSemTeto = plano("Sem teto", null, true);
    }

    // ---------- C1, C6, C7, C12: o vinculo ----------

    @Test
    @DisplayName("C1 - a empresa tem no maximo um plano vigente")
    void c1() throws Exception {
        liberar(empresaId, planoDeDois).andExpect(status().isOk());
        liberar(empresaId, planoDeCinco).andExpect(status().isOk());

        assertThat(subscriptionRepository.findAll())
            .filteredOn(s -> s.getCompany().getId().equals(empresaId))
            .filteredOn(s -> s.isVigente())
            .hasSize(1);
    }

    @Test
    @DisplayName("C6 - liberar vincula o plano e devolve o vinculo")
    void c6() throws Exception {
        liberar(empresaId, planoDeCinco)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value(planoDeCinco))
            .andExpect(jsonPath("$.limiteAnuncios").value(5));
    }

    @Test
    @DisplayName("C7 - trocar substitui o vigente, sem criar um segundo")
    void c7() throws Exception {
        liberar(empresaId, planoDeDois).andExpect(status().isOk());
        liberar(empresaId, planoDeCinco).andExpect(status().isOk());

        assertThat(subscriptionRepository.findByCompanyIdAndVigenteTrue(empresaId))
            .get()
            .satisfies(s -> assertThat(s.getPlan().getId()).isEqualTo(planoDeCinco));

        // A anterior nao some: ela e a trilha.
        assertThat(subscriptionRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("C12 - o vinculo registra quem liberou e quando")
    void c12() throws Exception {
        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        assertThat(subscriptionRepository.findByCompanyIdAndVigenteTrue(empresaId))
            .get()
            .satisfies(s -> {
                assertThat(s.getLiberadoPor()).isEqualTo(interno);
                assertThat(s.getLiberadoEm()).isNotNull();
            });
    }

    // ---------- C2, C3, C13, C15: o teto que o plano manda ----------

    @Test
    @DisplayName("C2 - plano de limite 2 nao deixa o terceiro anuncio no ar")
    void c2() throws Exception {
        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        publicar(empresa).andExpect(status().isOk());
        publicar(empresa).andExpect(status().isOk());
        publicar(empresa).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C3 - plano de limite nulo publica sem teto")
    void c3() throws Exception {
        liberar(empresaId, planoSemTeto).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            publicar(empresa).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("C13 - a quota da PJ reflete o limite do plano")
    void c13() throws Exception {
        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/announcements/me/quota")
                .header("Authorization", "Bearer " + tokenDe(empresa)))
            .andExpect(jsonPath("$.ilimitado").value(false))
            .andExpect(jsonPath("$.limite").value(2));
    }

    @Test
    @DisplayName("C15 - empresa aprovada e sem plano nao publica")
    void c15() throws Exception {
        assertThat(subscriptionRepository.findByCompanyIdAndVigenteTrue(empresaId)).isEmpty();

        publicar(empresa)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("plano")));
    }

    // ---------- C4 e C5: quem pode liberar ----------

    @Test
    @DisplayName("C4 - conta comum nao libera plano")
    void c4() throws Exception {
        mockMvc.perform(post("/api/v1/backoffice/companies/" + empresaId + "/plano")
                .header("Authorization", "Bearer " + tokenDe(empresa))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\": %d}".formatted(planoDeDois)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C5 - a empresa nao tem rota para mudar o proprio plano")
    void c5() throws Exception {
        // A unica rota de liberacao vive sob /backoffice, que o SecurityConfig
        // fecha por papel. Nao existe equivalente em /companies/me.
        mockMvc.perform(post("/api/v1/companies/me/plano")
                .header("Authorization", "Bearer " + tokenDe(empresa))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\": %d}".formatted(planoDeDois)))
            .andExpect(status().is4xxClientError());

        assertThat(subscriptionRepository.findByCompanyIdAndVigenteTrue(empresaId)).isEmpty();
    }

    // ---------- C8, C9, C10: o downgrade ----------

    @Test
    @DisplayName("C8 - downgrade nao inativa nenhum anuncio")
    void c8() throws Exception {
        liberar(empresaId, planoDeCinco).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            publicar(empresa).andExpect(status().isOk());
        }

        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        assertThat(announcementRepository.countByUserEmailAndActiveTrue(empresa)).isEqualTo(4);
    }

    @Test
    @DisplayName("C9 - depois do downgrade os anuncios continuam no acervo publico")
    void c9() throws Exception {
        liberar(empresaId, planoDeCinco).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            publicar(empresa).andExpect(status().isOk());
        }

        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/announcements").param("q", "Casa da spec 30"))
            .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("C10 - acima do teto depois do downgrade, publicar mais um e recusado")
    void c10() throws Exception {
        liberar(empresaId, planoDeCinco).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            publicar(empresa).andExpect(status().isOk());
        }

        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        publicar(empresa).andExpect(status().isConflict());
    }

    // ---------- C11: o aviso ----------

    @Test
    @DisplayName("C11 - o impacto traz os anuncios no ar e o limite do destino")
    void c11() throws Exception {
        liberar(empresaId, planoDeCinco).andExpect(status().isOk());

        for (int i = 0; i < 3; i++) {
            publicar(empresa).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/backoffice/companies/" + empresaId + "/plano/impacto")
                .param("planId", String.valueOf(planoDeDois))
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anunciosNoAr").value(3))
            .andExpect(jsonPath("$.limiteDestino").value(2))
            .andExpect(jsonPath("$.excede").value(true));
    }

    @Test
    @DisplayName("C11 - subir de plano nao acusa excesso")
    void c11Upgrade() throws Exception {
        liberar(empresaId, planoDeDois).andExpect(status().isOk());

        publicar(empresa).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/backoffice/companies/" + empresaId + "/plano/impacto")
                .param("planId", String.valueOf(planoSemTeto))
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(jsonPath("$.excede").value(false));
    }

    // ---------- C14: plano desativado ----------

    @Test
    @DisplayName("C14 - plano desativado nao pode ser liberado")
    void c14() throws Exception {
        Long desativado = plano("Plano fora de linha", 3, false);

        liberar(empresaId, desativado).andExpect(status().isBadRequest());

        assertThat(subscriptionRepository.findByCompanyIdAndVigenteTrue(empresaId)).isEmpty();
    }

    // ---------- apoio da tela da spec 31 ----------

    @Test
    @DisplayName("a listagem traz empresa aprovada, com plano ou sem ele")
    void listagemDeEmpresasAprovadas() throws Exception {
        mockMvc.perform(get("/api/v1/backoffice/companies/planos")
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(status().isOk())
            // Aprovada e ainda sem plano precisa aparecer: e o caso que a tela
            // existe para resolver.
            .andExpect(jsonPath("$.content[?(@.companyId == %d)].planoNome".formatted(empresaId))
                .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));

        liberar(empresaId, planoDeDois).andExpect(status().isOk());
        publicar(empresa).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/backoffice/companies/planos")
                .header("Authorization", "Bearer " + tokenDe(interno)))
            .andExpect(jsonPath("$.content[?(@.companyId == %d)].planoNome".formatted(empresaId))
                .value(org.hamcrest.Matchers.contains("Dois anuncios")))
            .andExpect(jsonPath("$.content[?(@.companyId == %d)].anunciosNoAr".formatted(empresaId))
                .value(org.hamcrest.Matchers.contains(1)));
    }

    // ---------- apoio ----------

    private ResultActions liberar(Long companyId, Long planId) throws Exception {
        return mockMvc.perform(post("/api/v1/backoffice/companies/" + companyId + "/plano")
            .header("Authorization", "Bearer " + tokenDe(interno))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\": %d}".formatted(planId)));
    }

    private Long plano(String nome, Integer limite, boolean ativo) {
        Plan plano = new Plan();

        plano.setNome(nome);
        plano.setPrecoMensal(BigDecimal.TEN);
        plano.setLimiteAnuncios(limite);
        plano.setAtivo(ativo);

        return planRepository.saveAndFlush(plano).getId();
    }

    /** Aprovada de proposito sem plano: e o estado que a C15 mede. */
    private void aprovarSemPlano(String email) {
        Company company = companyRepository.findByUserEmail(email).orElseThrow();

        company.setAnalysisStatus(AnalysisStatus.APROVADA);
        companyRepository.saveAndFlush(company);
    }

    private ResultActions publicar(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Casa da spec 30",
                  "description": "Anuncio da suite",
                  "image": "/api/v1/imagens/foto-de-teste.png",
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
                """));
    }

    private String cadastrar(String tipo) throws Exception {
        String email = "vin" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta da spec 30",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "%s",
                      "cnpj": "%014d"
                    }
                    """.formatted(
                        email,
                        String.format("11%09d", SEQUENCIA.getAndIncrement()),
                        tipo,
                        SEQUENCIA.getAndIncrement())))
            .andExpect(status().isCreated());

        return email;
    }

    private String cadastrarInterno() throws Exception {
        String email = cadastrar("PJ");

        com.example.demo.model.User user = userRepository.findByEmail(email).orElseThrow();

        user.setRole(com.example.demo.enums.UserRole.INTERNO);
        userRepository.saveAndFlush(user);

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
