package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.support.PostgresIntegrationTest;

/**
 * Cobre a spec 12-schema-versionado.
 *
 * O contexto Spring deste teste so sobe se o Flyway aplicou a migracao e o
 * Hibernate validou o mapeamento contra ela: a propria existencia da classe
 * rodando ja e parte da prova, e os testes conferem o resultado no catalogo.
 */
class SchemaVersionadoSpecTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    // ---------- C1 ----------

    @Test
    @DisplayName("C1 - ddl-auto vale validate")
    void c1() {
        assertThat(ddlAuto).isEqualTo("validate");
    }

    // ---------- C2 ----------

    @Test
    @DisplayName("C2 - a tabela de historico do Flyway existe")
    void c2() {
        assertThat(tabelaExiste("flyway_schema_history")).isTrue();
    }

    // ---------- C3 ----------

    @Test
    @DisplayName("C3 - as sete tabelas do dominio existem")
    void c3() {
        List<String> esperadas = List.of(
            "users", "announcements", "companies", "plans",
            "support_requests", "favorites", "announcement_views"
        );

        esperadas.forEach(tabela ->
            assertThat(tabelaExiste(tabela)).as(tabela).isTrue());
    }

    // ---------- C4, C5, C6 ----------

    @Test
    @DisplayName("C4 - a restricao unica do par CRECI existe")
    void c4() {
        assertThat(restricaoExiste("uk_company_creci")).isTrue();
    }

    @Test
    @DisplayName("C5 - a restricao unica de favoritos existe")
    void c5() {
        assertThat(restricaoExiste("uk_favorite_user_announcement")).isTrue();
    }

    @Test
    @DisplayName("C6 - a restricao unica de visualizacoes existe")
    void c6() {
        assertThat(restricaoExiste("uk_view_user_announcement")).isTrue();
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - o mapeamento JPA confere com o schema da migracao")
    void c7() {
        // Com ddl-auto=validate, divergencia entre entidade e schema derruba a
        // criacao do contexto. Chegar aqui com o contexto vivo e a prova.
        assertThat(ddlAuto).isEqualTo("validate");
        assertThat(userRepository.count()).isNotNegative();
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - uma conta gravada e relida atravessa o schema versionado")
    void c8() {
        User user = new User();

        user.setFullname("Conta sobre schema versionado");
        user.setEmail("v1@t.dev");
        user.setPhone("11988887777");
        user.setPassword(passwordEncoder.encode("Senha123"));
        user.setAccountType(AccountType.PF);
        user.setAccountStatus(AccountStatus.ATIVO);
        user.setRole(UserRole.COMUM);

        Long id = userRepository.save(user).getId();

        assertThat(userRepository.findById(id)).isPresent();

        userRepository.deleteById(id);
    }

    // ---------- C9 ----------

    @Test
    @DisplayName("C9 - o historico registra uma aplicacao bem-sucedida da versao 1")
    void c9() {
        Integer aplicacoes = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
            Integer.class
        );

        assertThat(aplicacoes).isEqualTo(1);
    }

    // ---------- apoio ----------

    private boolean tabelaExiste(String nome) {
        Integer total = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_tables WHERE schemaname = 'public' AND tablename = ?",
            Integer.class, nome
        );

        return total != null && total > 0;
    }

    private boolean restricaoExiste(String nome) {
        Integer total = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_constraint WHERE conname = ?",
            Integer.class, nome
        );

        return total != null && total > 0;
    }
}
