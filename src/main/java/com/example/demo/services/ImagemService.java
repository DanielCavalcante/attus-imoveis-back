package com.example.demo.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import lombok.RequiredArgsConstructor;

/**
 * Guarda e entrega as imagens do anuncio.
 *
 * O que torna isto seguranca, e nao interface: o servidor decide o nome do
 * objeto, decide o tipo pelo conteudo e nunca aceita endereco apontando para
 * fora. Trocar o campo da tela sem isto so esconderia a porta.
 */
@Service
@RequiredArgsConstructor
public class ImagemService {

    private final Storage storage;

    @Value("${attus.imagens.bucket}")
    private String bucket;

    @Value("${attus.imagens.tamanho-maximo:5MB}")
    private DataSize tamanhoMaximo;

    /**
     * Os tipos aceitos, e a assinatura de bytes de cada um.
     *
     * SVG fica de fora mesmo sendo imagem: SVG e documento com script dentro,
     * e servi-lo do nosso proprio dominio seria XSS hospedado por nos.
     */
    private enum Tipo {

        JPEG("image/jpeg", "jpg", new int[] {0xFF, 0xD8, 0xFF}),
        PNG("image/png", "png", new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
        /** WebP e RIFF....WEBP: os bytes 0-3 e 8-11, com o tamanho no meio. */
        WEBP("image/webp", "webp", new int[] {0x52, 0x49, 0x46, 0x46});

        final String contentType;
        final String extensao;
        final int[] assinatura;

        Tipo(String contentType, String extensao, int[] assinatura) {
            this.contentType = contentType;
            this.extensao = extensao;
            this.assinatura = assinatura;
        }

        boolean assina(byte[] inicio) {
            if (inicio.length < assinatura.length) {
                return false;
            }

            for (int i = 0; i < assinatura.length; i++) {
                if ((inicio[i] & 0xFF) != assinatura[i]) {
                    return false;
                }
            }

            if (this != WEBP) {
                return true;
            }

            // RIFF sozinho tambem e WAV e AVI: sem conferir "WEBP" no byte 8,
            // um audio passaria por imagem.
            return inicio.length >= 12
                && (inicio[8] & 0xFF) == 0x57
                && (inicio[9] & 0xFF) == 0x45
                && (inicio[10] & 0xFF) == 0x42
                && (inicio[11] & 0xFF) == 0x50;
        }
    }

    private static final String PREFIXO = "/api/v1/imagens/";

    /** Onde a imagem enviada fica acessivel. E isto que vai para o anuncio. */
    public static String caminhoDe(String nomeDoObjeto) {
        return PREFIXO + nomeDoObjeto;
    }

    /**
     * O objeto por tras de um caminho, ou nulo quando nao ha objeto.
     *
     * Nulo e resposta legitima: anuncio de antes da spec 32 guarda endereco
     * externo, que nao corresponde a nada no bucket.
     */
    public static String objetoDe(String caminho) {
        if (caminho == null || !caminho.startsWith(PREFIXO)) {
            return null;
        }

        return caminho.substring(PREFIXO.length());
    }

    /** O tipo do que foi guardado, para a galeria nao ter de adivinhar. */
    public String contentTypeDe(String nomeDoObjeto) {
        Blob blob = storage.get(BlobId.of(bucket, nomeDoObjeto));

        return blob == null || blob.getContentType() == null
            ? "image/jpeg"
            : blob.getContentType();
    }

    /**
     * Recebe o arquivo, confere o que ele e de verdade e guarda no bucket.
     *
     * Nada e gravado antes de as checagens passarem: recusa nao pode deixar
     * rastro, nem no bucket nem em lugar nenhum.
     */
    public String guardar(MultipartFile arquivo, String dono) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Envie um arquivo de imagem");
        }

        if (arquivo.getSize() > tamanhoMaximo.toBytes()) {
            throw new BusinessException(
                "A imagem deve ter no máximo %d MB".formatted(tamanhoMaximo.toMegabytes())
            );
        }

        byte[] bytes;

        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo enviado");
        }

        // A assinatura de bytes e quem decide. Content-Type e extensao sao
        // informados por quem sobe, e os dois mentem sem esforco nenhum.
        Tipo tipo = tipoPelaAssinatura(bytes).orElseThrow(() -> new BusinessException(
            "Formato não aceito. Envie JPEG, PNG ou WebP."
        ));

        // Nome gerado aqui: nome de quem sobe nao decide caminho nem extensao,
        // senao "../" e ".html" viram problema nosso.
        String nomeDoObjeto = "%s-%s.%s".formatted(
            UUID.randomUUID(), UUID.randomUUID(), tipo.extensao
        );

        BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucket, nomeDoObjeto))
            .setContentType(tipo.contentType)
            // Quem enviou fica gravado no objeto: sem isso, o identificador
            // devolvido aqui vira referencia que qualquer conta anexa ao
            // proprio anuncio.
            .setMetadata(java.util.Map.of("dono", dono))
            .build();

        storage.create(info, bytes);

        return nomeDoObjeto;
    }

    /** Os bytes e o tipo do que foi guardado, para a rota que serve. */
    public Blob buscar(String nomeDoObjeto) {
        Blob blob = storage.get(BlobId.of(bucket, nomeDoObjeto));

        if (blob == null || !blob.exists()) {
            throw new ResourceNotFoundException("Imagem não encontrada");
        }

        return blob;
    }

    /** Quem enviou esta imagem, se o objeto existir. */
    public Optional<String> donoDe(String nomeDoObjeto) {
        Blob blob = storage.get(BlobId.of(bucket, nomeDoObjeto));

        return blob == null
            ? Optional.empty()
            : Optional.ofNullable(blob.getMetadata()).map(m -> m.get("dono"));
    }

    /** Apagar o que ninguem mais referencia. Objeto ausente nao e erro. */
    public void apagar(String nomeDoObjeto) {
        storage.delete(BlobId.of(bucket, nomeDoObjeto));
    }

    private Optional<Tipo> tipoPelaAssinatura(byte[] bytes) {
        byte[] inicio = Arrays.copyOf(bytes, Math.min(bytes.length, 12));

        return List.of(Tipo.values()).stream()
            .filter(tipo -> tipo.assina(inicio))
            .findFirst();
    }
}
