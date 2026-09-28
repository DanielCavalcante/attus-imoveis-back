package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.exceptions.handler.GlobalExceptionHandler;
import com.example.demo.exceptions.models.ApiError;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/** Cobre a spec 13-falha-de-token-e-integridade. Cada teste cita o criterio que fecha. */
class FalhaDeTokenSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);
    private static final String MALFORMADO = "nao-e-um-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GlobalExceptionHandler handler;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private ListAppender<ILoggingEvent> registros;
    private Logger raiz;

    @BeforeEach
    void preparar() {
        userRepository.deleteAll();

        raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        registros = new ListAppender<>();
        registros.start();
        raiz.addAppender(registros);
    }

    @AfterEach
    void desligar() {
        raiz.detachAppender(registros);
    }

    // ---------- C1 e C2 ----------

    @Test
    @DisplayName("C1 - rota publica com token malformado responde 200")
    void c1() throws Exception {
        mockMvc.perform(get("/api/v1/announcements")
                .header("Authorization", "Bearer " + MALFORMADO))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C2 - rota publica com token expirado responde 200")
    void c2() throws Exception {
        mockMvc.perform(get("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenExpirado("qualquer@t.dev")))
            .andExpect(status().isOk());
    }

    // ---------- C3, C4, C5, C6 ----------

    @Test
    @DisplayName("C3 - rota protegida com token malformado responde 403")
    void c3() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + MALFORMADO))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C4 - rota protegida com assinatura invalida responde 403")
    void c4() throws Exception {
        String email = conta();
        String valido = jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());

        // Troca o ultimo caractere: estrutura intacta, assinatura quebrada.
        String adulterado = valido.substring(0, valido.length() - 1)
            + (valido.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + adulterado))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C5 - rota protegida com token expirado responde 403")
    void c5() throws Exception {
        String email = conta();

        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + tokenExpirado(email)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C6 - rota protegida com token de conta removida responde 403")
    void c6() throws Exception {
        String email = conta();
        String token = jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());

        userRepository.deleteAll();

        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    // ---------- C7 ----------

    @Test
    @DisplayName("C7 - token valido continua autenticando")
    void c7() throws Exception {
        String email = conta();
        String token = jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());

        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    // ---------- C8 ----------

    @Test
    @DisplayName("C8 - token invalido nao produz registro de nivel ERROR")
    void c8() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/me")
                .header("Authorization", "Bearer " + MALFORMADO))
            .andExpect(status().isForbidden());

        assertThat(registros.list)
            .filteredOn(evento -> evento.getLevel() == Level.ERROR)
            .isEmpty();
    }

    // ---------- C9 e C10 ----------

    /*
     * Violacao de integridade nao e alcancavel de forma deterministica por
     * endpoint: todo servico que escreve nas restricoes unicas consulta antes.
     * Ela existe para a corrida entre duas requisicoes, entao os dois criterios
     * se provam no handler -- simular a corrida produziria teste instavel.
     */

    @Test
    @DisplayName("C9 - violacao de integridade responde 409")
    void c9() {
        ResponseEntity<ApiError> resposta = handler.handleDataIntegrity(
            new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_company_creci\"")
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("C10 - a resposta 409 nao contem o texto da excecao do banco")
    void c10() {
        ResponseEntity<ApiError> resposta = handler.handleDataIntegrity(
            new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_company_creci\"")
        );

        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message())
            .doesNotContain("uk_company_creci")
            .doesNotContain("duplicate key")
            .doesNotContain("constraint");
    }

    // ---------- apoio ----------

    private String tokenExpirado(String email) {
        SecretKey chave = Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
            .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
            .signWith(chave, SignatureAlgorithm.HS256)
            .compact();
    }

    private String conta() {
        User user = new User();

        user.setFullname("Conta de teste");
        user.setEmail("t" + SEQUENCIA.getAndIncrement() + "@t.dev");
        user.setPhone(String.format("11%09d", SEQUENCIA.getAndIncrement()));
        user.setPassword(passwordEncoder.encode("Senha123"));
        user.setAccountType(AccountType.PF);
        user.setAccountStatus(AccountStatus.ATIVO);
        user.setRole(UserRole.COMUM);

        return userRepository.save(user).getEmail();
    }
}
