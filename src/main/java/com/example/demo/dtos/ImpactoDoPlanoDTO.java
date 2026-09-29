package com.example.demo.dtos;

/**
 * O que a troca de plano vai causar, antes de ela acontecer.
 *
 * Os numeros vem do servidor e nao de contagem montada na tela: a tela que
 * calcula o proprio aviso erra quando alguem publica entre o carregamento e a
 * confirmacao.
 *
 * `excede` e informativo. Nenhum anuncio sai do ar por causa dele -- a decisao
 * foi explicita: avisar, nao bloquear.
 */
public record ImpactoDoPlanoDTO(
    long anunciosNoAr,
    String planoAtual,
    Integer limiteAtual,
    String planoDestino,
    Integer limiteDestino,
    boolean excede
) {}
