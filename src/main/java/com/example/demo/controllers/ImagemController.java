package com.example.demo.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dtos.ImagemEnviadaDTO;
import com.example.demo.services.ImagemService;
import com.google.cloud.storage.Blob;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Envio e entrega das imagens do anuncio.
 *
 * A imagem e servida por aqui e nao pelo bucket, que fica privado: e o que
 * mantem o Content-Type e o nosniff sob nosso controle. O custo e banda
 * passando pela API, e a saida, se apertar, e URL assinada -- nao e abrir o
 * bucket.
 */
@Tag(name = "Imagens")
@RestController
@RequestMapping("/api/v1/imagens")
@RequiredArgsConstructor
public class ImagemController {

    private final ImagemService imagemService;

    @Operation(summary = "Envia uma imagem e devolve o caminho por onde ela sera servida")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagemEnviadaDTO> enviar(
        @RequestParam("arquivo") MultipartFile arquivo,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        String nome = imagemService.guardar(arquivo, userDetails.getUsername());

        return ResponseEntity.ok(new ImagemEnviadaDTO(nome, ImagemService.caminhoDe(nome)));
    }

    @Operation(summary = "Serve uma imagem enviada")
    @GetMapping("/{nome}")
    public ResponseEntity<byte[]> servir(@PathVariable String nome) {
        Blob blob = imagemService.buscar(nome);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(blob.getContentType()))
            // Sem isto o navegador pode adivinhar o tipo e tratar como HTML o
            // que guardamos como imagem -- e ai o nosso dominio executa o que
            // alguem subiu.
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
            .body(blob.getContent());
    }
}
