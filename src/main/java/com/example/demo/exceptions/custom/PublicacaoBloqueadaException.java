package com.example.demo.exceptions.custom;

/**
 * A conta nao pode publicar, e nenhuma vaga resolve.
 *
 * Responde 403, e nao o 409 da quota, porque as duas recusas mandam a pessoa
 * para lugares diferentes: 409 significa "libere uma vaga inativando algo", e
 * essa leitura seria falsa aqui -- o que falta e a aprovacao do cadastro.
 */
public class PublicacaoBloqueadaException extends RuntimeException {

    public PublicacaoBloqueadaException(String message) {
        super(message);
    }
}
