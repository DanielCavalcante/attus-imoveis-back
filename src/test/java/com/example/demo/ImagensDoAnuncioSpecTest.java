package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.demo.repositories.AnnouncementImageRepository;
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

/** Cobre a spec 34-varias-imagens-por-anuncio. Cada teste cita o criterio. */
class ImagensDoAnuncioSpecTest extends PostgresIntegrationTest {

    private static final AtomicLong SEQUENCIA = new AtomicLong(90000);

    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementImageRepository imagemRepository;

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
    private String outraConta;

    @BeforeEach
    void preparar() throws Exception {
        imagemRepository.deleteAll();
        announcementRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        dono = cadastrarPj();
        liberarParaAnunciar(dono);

        outraConta = cadastrarPj();
        liberarParaAnunciar(outraConta);
    }

    // ---------- C1 a C3: o conjunto e a ordem ----------

    @Test
    @DisplayName("C1 - a tabela guarda a ordem de cada foto")
    void c1() throws Exception {
        List<String> fotos = enviar(3, dono);

        publicar(fotos, null, dono).andExpect(status().isOk());

        assertThat(imagemRepository.findAll())
            .extracting(i -> i.getPosicao())
            .containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("C2 - criar com varias fotos grava todas")
    void c2() throws Exception {
        publicar(enviar(3, dono), null, dono).andExpect(status().isOk());

        assertThat(imagemRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("C3 - a ordem enviada e a ordem devolvida")
    void c3() throws Exception {
        List<String> fotos = enviar(3, dono);

        long id = idDe(publicar(fotos, null, dono));

        mockMvc.perform(get("/api/v1/announcements/" + id))
            .andExpect(jsonPath("$.imagens[0]").value(fotos.get(0)))
            .andExpect(jsonPath("$.imagens[1]").value(fotos.get(1)))
            .andExpect(jsonPath("$.imagens[2]").value(fotos.get(2)));
    }

    // ---------- C4, C5, C7, C8: a capa ----------

    @Test
    @DisplayName("C4 - um anuncio com varias fotos tem exatamente uma capa")
    void c4() throws Exception {
        publicar(enviar(3, dono), 1, dono).andExpect(status().isOk());

        assertThat(imagemRepository.findAll())
            .filteredOn(i -> i.isCapa())
            .hasSize(1);
    }

    @Test
    @DisplayName("C5 - sem capa indicada, a primeira e a capa")
    void c5() throws Exception {
        List<String> fotos = enviar(3, dono);

        long id = idDe(publicar(fotos, null, dono));

        mockMvc.perform(get("/api/v1/announcements/" + id))
            .andExpect(jsonPath("$.image").value(fotos.get(0)));
    }

    @Test
    @DisplayName("C7 - a listagem devolve a capa em `image`")
    void c7() throws Exception {
        List<String> fotos = enviar(3, dono);

        publicar(fotos, 2, dono).andExpect(status().isOk());

        // A forma da listagem nao muda: os cards continuam lendo `image`.
        mockMvc.perform(get("/api/v1/announcements").param("q", "Casa da spec 34"))
            .andExpect(jsonPath("$.content[0].image").value(fotos.get(2)));
    }

    @Test
    @DisplayName("C8 - trocar a capa nao mexe na ordem das outras")
    void c8() throws Exception {
        List<String> fotos = enviar(3, dono);

        long id = idDe(publicar(fotos, 0, dono));

        editar(id, fotos, 2, dono).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/announcements/" + id))
            .andExpect(jsonPath("$.image").value(fotos.get(2)))
            .andExpect(jsonPath("$.imagens[0]").value(fotos.get(0)))
            .andExpect(jsonPath("$.imagens[2]").value(fotos.get(2)));
    }

    // ---------- C6: o detalhe ----------

    @Test
    @DisplayName("C6 - o detalhe devolve todas as fotos")
    void c6() throws Exception {
        long id = idDe(publicar(enviar(4, dono), null, dono));

        mockMvc.perform(get("/api/v1/announcements/" + id))
            .andExpect(jsonPath("$.imagens.length()").value(4));
    }

    // ---------- C9 e C12: o que sai do bucket ----------

    @Test
    @DisplayName("C9 - remover uma foto na edicao apaga o objeto no bucket")
    void c9() throws Exception {
        List<String> fotos = enviar(3, dono);

        long id = idDe(publicar(fotos, null, dono));

        String removida = ImagemService.objetoDe(fotos.get(2));

        editar(id, fotos.subList(0, 2), null, dono).andExpect(status().isOk());

        assertThat(storage.get(BlobId.of(BUCKET_DE_TESTE, removida))).isNull();
    }

    @Test
    @DisplayName("C12 - excluir o anuncio apaga os objetos dele")
    void c12() throws Exception {
        List<String> fotos = enviar(2, dono);

        long id = idDe(publicar(fotos, null, dono));

        mockMvc.perform(delete("/api/v1/announcements/" + id)
                .header("Authorization", "Bearer " + tokenDe(dono)))
            .andExpect(status().isNoContent());

        for (String foto : fotos) {
            assertThat(storage.get(BlobId.of(BUCKET_DE_TESTE, ImagemService.objetoDe(foto))))
                .isNull();
        }
    }

    // ---------- C10, C11, C14: o que e recusado ----------

    @Test
    @DisplayName("C10 - passar do teto de fotos e recusado")
    void c10() throws Exception {
        publicar(enviar(13, dono), null, dono).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C11 - anexar foto enviada por outra conta e recusado")
    void c11() throws Exception {
        List<String> minhas = enviar(1, dono);
        List<String> dosOutros = enviar(1, outraConta);

        // A foto de um imovel de terceiro anunciada como sua: sem esta
        // checagem, o identificador devolvido pelo envio bastaria.
        publicar(List.of(minhas.get(0), dosOutros.get(0)), null, dono)
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C14 - recusa nao deixa nem linha nem anuncio")
    void c14() throws Exception {
        long anuncios = announcementRepository.count();
        long linhas = imagemRepository.count();

        publicar(enviar(13, dono), null, dono).andExpect(status().isBadRequest());

        assertThat(announcementRepository.count()).isEqualTo(anuncios);
        assertThat(imagemRepository.count()).isEqualTo(linhas);
    }

    // ---------- C13: o anuncio de antes ----------

    @Test
    @DisplayName("C13 - anuncio de antes da spec responde com uma imagem so")
    void c13() throws Exception {
        List<String> uma = enviar(1, dono);

        // Uma foto, pelo campo antigo: e o que a tela mandava ate a spec 33.
        long id = idDe(publicarComCampoUnico(uma.get(0), dono));

        mockMvc.perform(get("/api/v1/announcements/" + id))
            .andExpect(jsonPath("$.imagens.length()").value(1))
            .andExpect(jsonPath("$.imagens[0]").value(uma.get(0)))
            .andExpect(jsonPath("$.image").value(uma.get(0)));
    }

    // ---------- apoio ----------

    private List<String> enviar(int quantas, String conta) {
        return IntStream.range(0, quantas)
            .mapToObj(i -> {
                try {
                    String corpo = mockMvc.perform(multipart("/api/v1/imagens")
                            .file(new MockMultipartFile("arquivo", "f.png", "image/png", PNG))
                            .header("Authorization", "Bearer " + tokenDe(conta)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

                    return json.readTree(corpo).get("caminho").asText();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            })
            .toList();
    }

    private ResultActions publicar(List<String> fotos, Integer capa, String conta)
        throws Exception {

        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(conta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo(fotos, capa, null)));
    }

    private ResultActions publicarComCampoUnico(String foto, String conta) throws Exception {
        return mockMvc.perform(post("/api/v1/announcements")
            .header("Authorization", "Bearer " + tokenDe(conta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo(null, null, foto)));
    }

    private ResultActions editar(long id, List<String> fotos, Integer capa, String conta)
        throws Exception {

        return mockMvc.perform(put("/api/v1/announcements/" + id)
            .header("Authorization", "Bearer " + tokenDe(conta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo(fotos, capa, null)));
    }

    private String corpo(List<String> fotos, Integer capa, String unica) throws Exception {
        var no = json.createObjectNode();

        no.put("title", "Casa da spec 34");
        no.put("description", "Anuncio da suite");
        no.put("price", 300000);
        no.put("city", "João Pessoa");
        no.put("state", "PB");
        no.put("neighborhood", "Manaíra");
        no.put("street", "Rua do Sol");
        no.put("streetNumber", 100);
        no.put("cep", "58038-000");
        no.put("propertyType", "APARTMENT");
        no.put("reason", "SALE");
        no.put("rooms", 2);
        no.put("bathRooms", 1);
        no.put("area", 70);

        if (unica != null) {
            no.put("image", unica);
        }

        if (fotos != null) {
            var lista = no.putArray("imagens");

            fotos.forEach(lista::add);
        }

        if (capa != null) {
            no.put("capa", capa);
        }

        return json.writeValueAsString(no);
    }

    private long idDe(ResultActions acao) throws Exception {
        String corpo = acao.andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        return json.readTree(corpo).get("id").asLong();
    }

    private String cadastrarPj() throws Exception {
        String email = "gal" + SEQUENCIA.getAndIncrement() + "@t.dev";

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullname": "Conta da spec 34",
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
