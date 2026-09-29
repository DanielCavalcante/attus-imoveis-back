package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.repositories.UserRepository;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;

/**
 * Cobre C9 da spec 06. Em classe propria porque o limite chega por propriedade,
 * e provar que ele vem da configuracao exige um contexto com outro valor.
 */
@TestPropertySource(properties = "attus.quota.pf.ativos=1")
class QuotaConfiguravelSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(90000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("C9 - com o limite de ativos em 1, a PF e recusada no segundo anuncio")
    void c9() throws Exception {
        String email = cadastrarPf();

        criarAnuncio(email).andExpect(status().isOk());
        criarAnuncio(email).andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions criarAnuncio(String email)
        throws Exception {

        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Anuncio %d",
                  "description": "Anuncio criado pela suite",
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
                """.formatted(SEQUENCIA.getAndIncrement())));
    }

    private String cadastrarPf() throws Exception {
        String email = "k" + SEQUENCIA.getAndIncrement() + "@t.dev";

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
                      "accountType": "PF"
                    }
                    """.formatted(email, String.format("11%09d", SEQUENCIA.getAndIncrement()))))
            .andExpect(status().isCreated());

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
