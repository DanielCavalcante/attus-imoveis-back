package com.example.demo.controllers;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.EnderecoDTO;
import com.example.demo.services.EnderecoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * O endereco de um CEP.
 *
 * Autenticada -- cai no `anyRequest().authenticated()` do SecurityConfig --
 * porque rota aberta que chama servico externo e proxy de graca para quem
 * quiser, na nossa cota e com o nosso IP.
 */
@Tag(name = "Enderecos")
@RestController
@RequestMapping("/api/v1/enderecos")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService enderecoService;

    @Operation(summary = "Endereco a partir do CEP")
    @GetMapping("/{cep}")
    public ResponseEntity<EnderecoDTO> porCep(@PathVariable String cep) {
        return ResponseEntity.ok()
            // CEP nao muda de bairro: o navegador nao precisa perguntar duas
            // vezes na mesma sessao, e nos tambem nao.
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .body(enderecoService.buscar(cep));
    }
}
