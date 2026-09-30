package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.SubscriptionRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.ImagemService;
import com.example.demo.services.JwtService;
import com.example.demo.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;

/** Cobre a spec 32-imagem-enviada-e-nao-linkada. Cada teste cita o criterio. */
class ImagemDoAnuncioSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(80000);

    /** PNG de 1x1 valido: assinatura de verdade, nao bytes inventados. */
    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    );

    private static final byte[] JPEG = new byte[] {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Storage storage;

    private final ObjectMapper json = new ObjectMapper();

    private String dono;

    @BeforeEach
    void preparar() throws Exception {
        announcementRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        dono = cadastrarPj();
        liberarParaAnunciar(dono);
    }

    // ---------- C1 e C2: quem envia ----------

    @Test
    @DisplayName("C1 - conta autenticada envia imagem e recebe o caminho de volta")
    void c1() throws Exception {
        enviar(PNG, "foto.png", "image/png")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").isString())
            .andExpect(jsonPath("$.caminho").value(
                org.hamcrest.Matchers.startsWith("/api/v1/imagens/")));
    }

    @Test
    @DisplayName("C2 - envio sem token e recusado")
    void c2() throws Exception {
        mockMvc.perform(multipart("/api/v1/imagens")
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", PNG)))
            .andExpect(status().is4xxClientError());
    }

    // ---------- C3 a C6: o que nao entra ----------

    @Test
    @DisplayName("C3 - arquivo que nao e imagem e recusado")
    void c3() throws Exception {
        byte[] pdf = "%PDF-1.7 documento".getBytes(StandardCharsets.UTF_8);

        enviar(pdf, "contrato.pdf", "application/pdf")
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C4 - SVG e recusado mesmo declarando-se imagem")
    void c4() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>1</script></svg>"
            .getBytes(StandardCharsets.UTF_8);

        // Declarado como imagem, e e: mas e documento com script dentro, e
        // servi-lo do nosso dominio seria XSS hospedado por nos.
        enviar(svg, "mapa.svg", "image/svg+xml")
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C5 - arquivo que se diz PNG mas nao e PNG e recusado")
    void c5() throws Exception {
        byte[] mentiroso = "<html><script>alert(1)</script></html>"
            .getBytes(StandardCharsets.UTF_8);

        // Content-Type e extensao mentem sem esforco; a assinatura de bytes e
        // quem decide.
        enviar(mentiroso, "foto.png", "image/png")
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C6 - arquivo acima do limite e recusado")
    void c6() throws Exception {
        byte[] gigante = new byte[6 * 1024 * 1024];

        System.arraycopy(PNG, 0, gigante, 0, PNG.length);

        enviar(gigante, "enorme.png", "image/png")
            .andExpect(status().is4xxClientError());
    }

    // ---------- C7 e C8: o que a recusa deixa, e quem nomeia ----------

    @Test
    @DisplayName("C7 - recusa nao deixa objeto no bucket")
    void c7() throws Exception {
        long antes = objetosNoBucket();

        enviar("nada disso".getBytes(StandardCharsets.UTF_8), "x.png", "image/png")
            .andExpect(status().isBadRequest());

        assertThat(objetosNoBucket()).isEqualTo(antes);
    }

    @Test
    @DisplayName("C8 - o nome do objeto e gerado pelo servidor")
    void c8() throws Exception {
        String nome = nomeDoEnviado(PNG, "../../etc/passwd.html", "image/png");

        assertThat(nome)
            .doesNotContain("passwd")
            .doesNotContain("..")
            .doesNotContain("/")
            .endsWith(".png");
    }

    // ---------- C9 e C10: como a imagem e servida ----------

    @Test
    @DisplayName("C9 - a entrega devolve o Content-Type do que foi guardado")
    void c9() throws Exception {
        String nome = nomeDoEnviado(JPEG, "foto.jpg", "image/jpeg");

        mockMvc.perform(get("/api/v1/imagens/" + nome))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    @DisplayName("C10 - a entrega traz nosniff")
    void c10() throws Exception {
        String nome = nomeDoEnviado(PNG, "foto.png", "image/png");

        mockMvc.perform(get("/api/v1/imagens/" + nome))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // ---------- C11 a C13: a porta que se fecha, e a que fica ----------

    @Test
    @DisplayName("C11 - criar anuncio com endereco externo e recusado")
    void c11() throws Exception {
        mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoComImagem("https://images.unsplash.com/qualquer.jpg")))
            .andExpect(status().isBadRequest());

        assertThat(announcementRepository.count()).isZero();
    }

    @Test
    @DisplayName("C12 - editar trocando por endereco externo e recusado")
    void c12() throws Exception {
        String nome = nomeDoEnviado(PNG, "foto.png", "image/png");

        String corpo = mockMvc.perform(post("/api/v1/announcements")
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoComImagem(ImagemService.caminhoDe(nome))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        long id = json.readTree(corpo).get("id").asLong();

        mockMvc.perform(put("/api/v1/announcements/" + id)
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoComImagem("http://qualquer.host/foto.jpg")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C12 - editar mantendo o endereco que ja era dele e permitido")
    void c12MantemOProprio() throws Exception {
        // Anuncio anterior a spec 32: a imagem dele e um endereco externo, e
        // devolve-lo inalterado nao e apontar para outro host -- e preservar.
        // Sem isto, esses anuncios ficavam impossiveis de editar, ate para
        // corrigir o titulo.
        com.example.demo.model.Announcement antigo = anuncioComEnderecoExterno();
        long id = announcementRepository.saveAndFlush(antigo).getId();

        mockMvc.perform(put("/api/v1/announcements/" + id)
                .header("Authorization", "Bearer " + tokenDe(dono))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoComImagem("https://images.unsplash.com/antigo.jpg")
                    .replace("Casa da spec 32", "Titulo corrigido")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Titulo corrigido"));
    }

    /** Gravado por baixo do contrato novo, como os que ja existiam. */
    private com.example.demo.model.Announcement anuncioComEnderecoExterno() {
        com.example.demo.model.Announcement antigo = new com.example.demo.model.Announcement();

        antigo.setTitle("Anuncio de antes da spec 32");
        antigo.setDescription("Gravado quando o campo aceitava link");
        antigo.setImage("https://images.unsplash.com/antigo.jpg");
        antigo.setPrice(new java.math.BigDecimal("300000"));
        antigo.setCity("João Pessoa");
        antigo.setState("PB");
        antigo.setStreet("Rua do Sol");
        antigo.setStreetNumber(100);
        antigo.setCep("58038-000");
        antigo.setPropertyType(com.example.demo.enums.PropertyType.APARTMENT);
        antigo.setReason(com.example.demo.enums.ReasonType.SALE);
        antigo.setRooms(2);
        antigo.setBathRooms(1);
        antigo.setArea(new java.math.BigDecimal("70"));
        antigo.setUser(userRepository.findByEmail(dono).orElseThrow());

        return antigo;
    }

    @Test
    @DisplayName("C13 - anuncio antigo com endereco externo continua sendo devolvido")
    void c13() throws Exception {
        long id = announcementRepository.saveAndFlush(anuncioComEnderecoExterno()).getId();

        mockMvc.perform(get("/api/v1/announcements/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.image").value("https://images.unsplash.com/antigo.jpg"));
    }

    // ---------- C14 e C15: onde os bytes moram ----------

    @Test
    @DisplayName("C14 - a imagem vive no bucket, nao no processo")
    void c14() throws Exception {
        String nome = nomeDoEnviado(PNG, "foto.png", "image/png");

        // Lido direto do bucket, por fora da aplicacao: se estivesse no disco
        // do conteiner ou em memoria, nao estaria aqui -- e e isto que faz a
        // imagem sobreviver ao conteiner ser recriado.
        assertThat(storage.get(BlobId.of(BUCKET_DE_TESTE, nome))).isNotNull();
    }

    @Test
    @DisplayName("C15 - o mesmo cliente e o mesmo codigo falam com o emulador")
    void c15() {
        // O bean injetado aqui e o de producao, configurado por propriedade
        // para apontar ao emulador. Caminho de codigo separado para teste
        // deixaria o de producao sem prova nenhuma.
        assertThat(storage.get(BUCKET_DE_TESTE)).isNotNull();
    }

    // ---------- apoio ----------

    private ResultActions enviar(byte[] bytes, String nome, String contentType) throws Exception {
        return mockMvc.perform(multipart("/api/v1/imagens")
            .file(new MockMultipartFile("arquivo", nome, contentType, bytes))
            .header("Authorization", "Bearer " + tokenDe(dono)));
    }

    private String nomeDoEnviado(byte[] bytes, String nome, String contentType) throws Exception {
        String corpo = enviar(bytes, nome, contentType)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        return json.readTree(corpo).get("nome").asText();
    }

    private long objetosNoBucket() {
        return storage.list(BUCKET_DE_TESTE).streamAll().count();
    }

    private String corpoComImagem(String image) {
        return """
            {
              "title": "Casa da spec 32",
              "description": "Anuncio da suite",
              "image": "%s",
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
            """.formatted(image);
    }

    private String cadastrarPj() throws Exception {
        String email = "img" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta da spec 32",
                      "email": "%s",
                      "phone": "%s",
                      "password": "Senha123",
                      "confirmPassword": "Senha123",
                      "photo": "",
                      "accountType": "PJ",
                      "cnpj": "%014d"
                    }
                    """.formatted(
                        email,
                        String.format("11%09d", SEQUENCIA.getAndIncrement()),
                        SEQUENCIA.getAndIncrement())))
            .andExpect(status().isCreated());

        return email;
    }

    private String tokenDe(String email) {
        return jwtService.generateToken(userRepository.findByEmail(email).orElseThrow());
    }
}
