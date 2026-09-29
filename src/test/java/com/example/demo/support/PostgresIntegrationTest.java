package com.example.demo.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.repositories.CompanyRepository empresasDoTeste;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.repositories.PlanRepository planosDoTeste;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.repositories.SubscriptionRepository assinaturasDoTeste;

    /**
     * Deixa esta conta apta a anunciar: empresa aprovada e plano vigente.
     *
     * Sao dois portoes, e os dois nasceram de defeito real -- a spec 28 porque
     * a analise do backoffice nao mandava em nada, e a spec 30 porque os
     * planos eram catalogo sem assinante. Suite que anuncia como PJ declara as
     * duas pre-condicoes aqui, no preparo, para quem le o teste ver que elas
     * existem em vez de a publicacao funcionar por acidente.
     *
     * O plano e o ilimitado: quem mede quota tem os seus proprios, e quem nao
     * mede nao deve esbarrar num teto que a suite nao escolheu.
     *
     * Conta PF nao tem empresa, e chamar isto para ela nao faz nada.
     */
    protected void liberarParaAnunciar(String email) {
        empresasDoTeste.findByUserEmail(email).ifPresent(empresa -> {
            empresa.setAnalysisStatus(com.example.demo.enums.AnalysisStatus.APROVADA);
            empresasDoTeste.saveAndFlush(empresa);

            if (assinaturasDoTeste.findByCompanyIdAndVigenteTrue(empresa.getId()).isEmpty()) {
                com.example.demo.model.Subscription assinatura =
                    new com.example.demo.model.Subscription();

                assinatura.setCompany(empresa);
                assinatura.setPlan(planoIlimitado());
                assinatura.setLiberadoPor("suite@teste.dev");

                assinaturasDoTeste.saveAndFlush(assinatura);
            }
        });
    }

    /** Um plano sem teto, criado uma vez e reaproveitado pelas suites. */
    private com.example.demo.model.Plan planoIlimitado() {
        return planosDoTeste.findByNome("Ilimitado do teste").orElseGet(() -> {
            com.example.demo.model.Plan plano = new com.example.demo.model.Plan();

            plano.setNome("Ilimitado do teste");
            plano.setPrecoMensal(java.math.BigDecimal.ZERO);
            plano.setLimiteAnuncios(null);
            plano.setAtivo(true);

            return planosDoTeste.saveAndFlush(plano);
        });
    }
}
