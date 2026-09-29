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

    /**
     * Aprova a empresa desta conta, para que ela possa anunciar.
     *
     * Desde a spec 28 publicar exige analise aprovada, e uma PJ recem-cadastrada
     * nasce em NAO_ENVIADO. Suite que anuncia como PJ declara essa pre-condicao
     * aqui, no preparo -- e quem le o teste ve que ela existe, em vez de a
     * publicacao funcionar por acidente de um portao que nao checava nada.
     *
     * Conta PF nao tem empresa, e chamar isto para ela nao faz nada.
     */
    protected void aprovarEmpresa(String email) {
        empresasDoTeste.findByUserEmail(email).ifPresent(empresa -> {
            empresa.setAnalysisStatus(com.example.demo.enums.AnalysisStatus.APROVADA);
            empresasDoTeste.saveAndFlush(empresa);
        });
    }
}
