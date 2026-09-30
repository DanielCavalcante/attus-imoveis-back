package com.example.demo.exceptions.custom;

/**
 * Nao foi possivel perguntar a quem sabe.
 *
 * Responde 503 e nao 500: 500 diria que o defeito e nosso, e mandaria quem
 * consome procurar erro onde nao ha. 503 diz o que e -- dependencia externa
 * fora do ar -- e convida a tentar de novo, que e a acao certa.
 */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String message) {
        super(message);
    }
}
