package com.example.demo.dtos;

/**
 * O que o envio devolve.
 *
 * `nome` e a referencia que o anuncio guarda; `caminho` e por onde a tela
 * mostra. Devolver os dois evita que o front monte o caminho por concatenacao
 * e fique preso ao formato dele.
 */
public record ImagemEnviadaDTO(String nome, String caminho) {}
